package com.xkcoding.helloworld.service.utils;

import cn.hutool.json.JSONObject;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.LocalVariableTableParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@Aspect
public class LocalCachePutAspect {

    private LocalCacheUtils localCacheUtils = new LocalCacheUtils();

    /**
     * spring 参数名称解析器
     */
    private static final ParameterNameDiscoverer LOCAL_VARIABLE_TABLE_PARAMETER_NAME_DISCOVERER
        = new LocalVariableTableParameterNameDiscoverer();
    /**
     * spring el 表达式解析解
     */
    private static final ExpressionParser SPEL_EXPRESSION_PARSER = new SpelExpressionParser();

    /**
     * 切入点
     * 切入点为包路径下的：execution(public * org.xx.xx.xx.controller..*(..))：
     * org.xx.xx.xx.Controller包下任意类任意返回值的 public 的方法
     * <p>
     * 切入点为注解的： @annotation(VisitPermission)
     * 存在 LocalCacheAnnotation 注解的方法
     */
    @Pointcut("@annotation(com.xkcoding.helloworld.service.utils.LocalCachePutAnnotation)")
    public void putCache(){

    }

    @AfterReturning(returning="res",value = "putCache()")
    public void toAfter(JoinPoint joinPoint,Object res) throws NoSuchMethodException {
        String methodName = joinPoint.getSignature().getName();
        Object[] args1 = joinPoint.getArgs();
        Class<?> classTarget = joinPoint.getTarget().getClass();
        Class<?>[] par = ((MethodSignature) joinPoint.getSignature()).getParameterTypes();
        Method targetMethod = classTarget.getMethod(methodName, par);
        String[] parameterNames = LOCAL_VARIABLE_TABLE_PARAMETER_NAME_DISCOVERER.getParameterNames(targetMethod);
        //获取注解信息
        LocalCachePutAnnotation annotation = targetMethod.getAnnotation(LocalCachePutAnnotation.class);
        if (annotation == null) {
            return;
        }
        String cacheKey = parseLockName(args1, annotation, parameterNames);

        if(annotation.isUpdateData()){
            boolean simpleType = isSimpleType(res.getClass());
            if(!simpleType){
                JSONObject jsonObject = new JSONObject(res);
                res = jsonObject.toString();
            }
            localCacheUtils.update(cacheKey,res,annotation.expireTime());
            return;
        }
        localCacheUtils.delete(cacheKey,true);

    }


    private String parseLockName(Object[] args, LocalCachePutAnnotation localCacheAnnotation, String[] parameterNames) {
        LocalCachePutExtractParamAnnotation[] extractParams = localCacheAnnotation.value();
        if (extractParams.length == 0) {
           return localCacheAnnotation.key();
        }
        List<String> fieldValues = new ArrayList<>();
        Map<String, Object> paramNameMap = buildParamMap(args, parameterNames);
        for (LocalCachePutExtractParamAnnotation extractParam : extractParams) {
            String paramName = extractParam.paramName();
            Object paramValue = paramNameMap.get(paramName);
            String springEL = extractParam.fieldName();
            String paramFieldValue = "";
            if (!StringUtils.isEmpty(springEL)) {
                Expression expression = SPEL_EXPRESSION_PARSER.parseExpression(springEL);
                paramFieldValue = expression.getValue(paramValue).toString();
            } else {
                if (isSimpleType(paramValue.getClass())) {
                    paramFieldValue = String.valueOf(paramValue);
                }
            }
            fieldValues.add(paramFieldValue);
        }
        return String.format(localCacheAnnotation.key(), fieldValues.toArray());
    }


    /**
     * 基本类型 int, double, float, long, short, boolean, byte, char， void.
     */
    private static boolean isSimpleType(Class<?> clazz) {
        return clazz.isPrimitive()
            || clazz.equals(Long.class)
            || clazz.equals(Integer.class)
            || clazz.equals(String.class)
            || clazz.equals(Double.class)
            || clazz.equals(Short.class)
            || clazz.equals(Byte.class)
            || clazz.equals(Character.class)
            || clazz.equals(Float.class)
            || clazz.equals(Boolean.class);
    }


    /**
     * 构建请求参数map
     * @param args 参数列表
     * @param parameterNames 参数名称列表
     * @return key:参数名称 value:参数值
     */
    private Map<String, Object> buildParamMap(Object[] args, String[] parameterNames) {
        Map<String, Object> paramNameMap = new HashMap<>();
        for (int i = 0; i < parameterNames.length; i++) {
            paramNameMap.put(parameterNames[i], args[i]);
        }
        return paramNameMap;
    }
}
