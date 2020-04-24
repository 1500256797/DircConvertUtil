package com.cesec.springboot.common.configure;

import com.cesec.springboot.common.utils.DircConvertUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.executor.resultset.ResultSetHandler;
import org.apache.ibatis.plugin.*;

import java.sql.Statement;
import java.util.List;
import java.util.Properties;

/**
 * ClassName: ResultSetConvert
 * Description: 自定义查询结果集拦截器
 * Author : zzq
 * Date : 2020/3/25 16:08
 * Version : 1.1
 **/
@Slf4j
@Intercepts({@Signature(type = ResultSetHandler.class, method = "handleResultSets", args = {Statement.class})})
public class ResultSetConvert implements Interceptor {
    /**
     * 是否转换开关
     */
    private static ThreadLocal<Boolean> convertStatus = new ThreadLocal<>();

    /**
     * 是否刷新开关
     */
    private static ThreadLocal<Boolean> freshStatus = new ThreadLocal<>();

    /**
     * 是否刷新开关
     */
    private static ThreadLocal<Boolean> freshLabelStatus = new ThreadLocal<>();

    /**
     * 翻译几次
     */
    private static ThreadLocal<Integer> convertCount = new ThreadLocal<>();

    /**
     * 翻译作用域
     */
    private static ThreadLocal<String> convertScope = new ThreadLocal<>();


    //忽略转换的文件后缀
    private final String methodSuffix = "_Dc";

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        List<Object> result = (List<Object>) invocation.proceed();
        if(convertStatus.get() != null && convertStatus.get()){
            StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
            for (StackTraceElement stackTraceElement : stackTrace) {
                if(stackTraceElement.getMethodName().contains(methodSuffix)) return result;
            }
            log.info("准备翻译拦截到的结果集："+result);
            if(convertScope.get()!=null){
                DircConvertUtils.convertDicInfo(result,false,convertScope.get());
            }else{
                DircConvertUtils.convertDicInfo(result,false);
            }
            //翻译一次减掉一次
            if(convertCount.get()!=null){
                Integer count = convertCount.get()-1;
                log.info("翻译次数减1，剩余次数"+"剩余翻译次数："+count);
                convertCount.set(count);
                if(count<=0){
                    log.warn("当前无剩余翻译次数："+convertCount.get());
                    close();
                }
            }else {
                close();
            }
        }
        return result;
    }
    /**
     * 清空
     */
    private static void close(){
        convertStatus.remove();
        freshStatus.remove();
    }

    /**
     * 跳过转译
     */
    public static void skip(){
        convertStatus.set(false);
    }


    /**
     * 启动字典表翻译，默认是将编码翻译成中文
     * 使用方法类似于pageHelper,在调用方法之前用。
     */
    public static void onNoFresh( ){
        freshStatus.set(false);
        convertStatus.set(true);
    }

    /**
     * 设置翻译次数
     */
    public static void onWithFresh( Integer times){
        skip();//跳过对字典是翻译过程
        freshStatus.set(true);
        //是否刷新字典
        if (freshStatus.get() != null && freshStatus.get()) {
            DircConvertUtils.fresh();
        }
        convertStatus.set(true);
        convertCount.set(times);
    }

    /**
     * 设置翻译次数 及 翻译作用域
     */
    public static void onWithFresh( Integer times,String scope){
        skip();//跳过对字典是翻译过程
        freshStatus.set(true);
        //是否刷新字典
        if (freshStatus.get() != null && freshStatus.get()) {
            DircConvertUtils.fresh();
        }
        convertStatus.set(true);
        convertCount.set(times);
        convertScope.set(scope);
    }
    /**
     * 启动字典表翻译，默认是将编码翻译成中文
     * 使用方法类似于pageHelper,在调用方法之前用。
     */
    public static void onWithFresh(){

        skip();//跳过对字典是翻译过程
        freshStatus.set(true);
        //是否刷新字典
        if (freshStatus.get() != null && freshStatus.get()) {
            DircConvertUtils.fresh();
        }
        convertStatus.set(true);
    }
    /**
     * 设置当前翻译次数
     */
    public static void setConvertCount(Integer times){
        //设置当前翻译次数
        convertCount.set(times);
    }

    //主要翻译标签
    public static void freshLabel(String treeCode ){
        skip();//跳过对标签字典的翻译过程
        freshLabelStatus.set(true);
        //是否刷新标签字典
        if (freshLabelStatus.get() != null && freshLabelStatus.get()) {
            DircConvertUtils.freshLabel(treeCode);//先刷新标签表映射关系
            DircConvertUtils.fresh();//再检查字典有无更新
        }
        //convertStatus.set(true);
    }

    @Override
    public Object plugin(Object o) {
        return Plugin.wrap(o, this);
    }

    @Override
    public void setProperties(Properties properties) {

    }
}
