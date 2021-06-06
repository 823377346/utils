package com.xkcoding.helloworld.service.utils;

import cn.hutool.json.JSONObject;
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
import java.util.concurrent.CompletableFuture;

@Component
@Aspect
public class LocalCacheGetAspect {

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
    @Pointcut("@annotation(com.xkcoding.helloworld.service.utils.LocalCacheGetAnnotation)")
    private void getCache(){

    }



    @Around(value = "getCache()")
    public Object toAround(ProceedingJoinPoint joinPoint) throws Throwable {

        Object proceed = null;
        Object[] args = joinPoint.getArgs();
        try {
            //从切面织入点处通过反射机制获取织入点处的方法
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            //获取切入点所在的方法
            Method method = signature.getMethod();


            String methodName = joinPoint.getSignature().getName();
            Class<?> classTarget = joinPoint.getTarget().getClass();
            Class<?>[] par = ((MethodSignature) joinPoint.getSignature()).getParameterTypes();
            Method targetMethod = classTarget.getMethod(methodName, par);
            String[] parameterNames = LOCAL_VARIABLE_TABLE_PARAMETER_NAME_DISCOVERER.getParameterNames(targetMethod);
            //获取注解信息
            LocalCacheGetAnnotation annotation = method.getAnnotation(LocalCacheGetAnnotation.class);
            if (annotation == null) {
                return null;
            }

            String cacheKey = parseLockName(args, annotation, parameterNames);
            Object resValue = localCacheUtils.get(cacheKey);

            if(resValue != null){
                ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
                HttpServletResponse response = attributes.getResponse();
                response.setCharacterEncoding("UTF-8");
                response.setContentType("application/json; charset=utf-8");
                PrintWriter out = null;
                try {
                    if(!isSimpleType(resValue.getClass())){
                        JSONObject jsonObject = new JSONObject(resValue);
                        resValue = jsonObject.toString();
                    }

                    out = response.getWriter();
                    out.print(resValue);

                    boolean updateData = annotation.isUpdateData();
                    if(updateData){
                        CompletableFuture.runAsync(()->{
                            try {
//                                Object newProceed = joinPoint.proceed(args);

                                Object newProceed = targetMethod.invoke(classTarget.newInstance(), args);
                                localCacheUtils.put(cacheKey,newProceed,annotation.expireTime());
                            } catch (Throwable throwable) {
                                throwable.printStackTrace();
                            }
                        });
                    }
                    return null;
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            proceed = joinPoint.proceed(args);

            if(proceed != null){
                localCacheUtils.put(cacheKey,proceed,annotation.expireTime());
            }
        } catch (Throwable throwable) {
            proceed = joinPoint.proceed(args);
            throwable.printStackTrace();
        }
        return proceed;
    }


    private String parseLockName(Object[] args, LocalCacheGetAnnotation localCacheAnnotation, String[] parameterNames) {
        LocalCacheGetExtractParamAnnotation[] extractParams = localCacheAnnotation.value();
        if (extractParams.length == 0) {
           return localCacheAnnotation.key();
        }
        List<String> fieldValues = new ArrayList<>();
        Map<String, Object> paramNameMap = buildParamMap(args, parameterNames);
        for (LocalCacheGetExtractParamAnnotation extractParam : extractParams) {
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
