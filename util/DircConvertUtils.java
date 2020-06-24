package com.cesec.springboot.common.utils;

import com.cesec.springboot.system.entity.TbDirc;
import com.cesec.springboot.system.entity.TbLabelField;
import com.cesec.springboot.system.mapper.TbDircMapper;
import com.cesec.springboot.system.mapper.TbLabelFieldMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.yaml.snakeyaml.Yaml;
import javax.annotation.PostConstruct;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * ClassName: DircConvertUtils
 * Author : zzq
 **/
@Slf4j
public class DircConvertUtils {
    //字典表： Map<type, Map<key, value>> 例如：Map<"10101", Map<"01", "流动人口">>
    private static final  Map<String, Map<String, Object>> DIC_INFO =new ConcurrentHashMap<>();
    //字典表信息(反转)，主要用于将中文转成编码 例如：Map<10101, Map<"流动人口"，"01" >>
    private static final  Map<String, Map<String, Object>> DIC_INFO_OVERTURN = new ConcurrentHashMap<>();
    //实体成员变量名与数据表类型映射关系：Map<fieldName,f_Type>  例如：Map<"sex","10103">
    private static final  Map<String, String> FIELDNAME_FTYPE_MAP = new HashMap<>();
    //域 字段 字典类型映射
    private static final Map<String, Map<String, String>> SCOPE_FIELDNAME_FTYPE_MAP = new HashMap<>(60);
    //标签约束  实体成员变量名与数据表类型映射关系：Map<label_tree_id,Map<fieldName,f_Type>>
    //例如：Map<"8ebba76af8c64a85be9510c060e0ebe5",Map<"sex","10103">>
    private static final Map<String, Map<String, String>> LABELROWID_FIELDNAME_FTYPE_MAP = new ConcurrentHashMap<>(60);

    //字典表 key字段名 value字段名
    private static final String KEY_NAME = "keyName";
    private static final String VALUE_NAME = "valueName";
    //字段 字典类型映射文件
    private static final String YAML_NAME = "test.yml";

    private static  TbDircMapper tbDircMapper;
    @Autowired
    private  TbDircMapper autowiredTbDircMapper;
    private static TbLabelFieldMapper tbLabelFieldMapper;
    @Autowired
    private TbLabelFieldMapper autowiredTbLabelFieldMapper;

    private static  List<TbDirc> tbDircList ;


    /**
     * @Description 自定义初始化bean
     * @param
     * @return void
     */
    @PostConstruct
    public  void init(){
        tbDircMapper = autowiredTbDircMapper;
        tbLabelFieldMapper = autowiredTbLabelFieldMapper;
        tbDircList = tbDircMapper.selectAll(); //字典表全表扫描 like "1%"
        //tbDircList = tbDircMapper.selectAll().stream().filter(tbDirc -> FIELDNAME_FTYPE_MAP.containsValue(tbDirc.getFType())).collect(Collectors.toList()); //字典表全表扫描
        if(tbDircList==null){
            log.warn("字典翻译工具类#请确定字典表中有无数据");
            return;
        }
        Set<String> allDircType = tbDircList.stream().filter(item->item!=null&&null!=item.getFType()&&null!=item.getFKey()&&null!=item.getFValue()).map(TbDirc::getFType).collect(Collectors.toSet()); //获取字典类型set
        //根据字典类型组成相应kv
        allDircType.stream().filter(item->item!=null).forEach(item->{
            List<Map<String, Object>> l2 = selectDicInfoByType(item.toString());
            if(l2.isEmpty()){
                return;//同continue
            }
            DircConvertUtils.loadDircByColType(l2, item.toString());//初始化字典
            //DircConvertUtils.loadDicInfoByColTypeOverturn(l2, item.toString());//初始化反转字典
        });
    }


    /**
     * @Description 根据标签主键刷新标签字段与字典类型映射
     * @param treeCodeRowId 标签主键
     * @return void
     */
    public static void freshLabel(String treeCodeRowId){
        //获取某个标签的所有字段信息
        List<TbLabelField> labelFieldDircCodes = tbLabelFieldMapper.selectByLabelTreeCodeRowId(treeCodeRowId);
        //某个label_tree_id的filed 和 dirccode组装成map
        Map<String, String> labelFieldDircCodeMap = new HashMap<>();
        labelFieldDircCodes.stream().forEach(item->{
            labelFieldDircCodeMap.put(item.getLabelField(), item.getDircCode());
        });
        //例如： <"8ebba76af8c64a85be9510c060e0ebe5",<crime_type,10217>>
        LABELROWID_FIELDNAME_FTYPE_MAP.put(treeCodeRowId, labelFieldDircCodeMap);
//      LabelRowId_FieldName_Ftype_Map.set();
    }


    /**
     * @Description 根据获取字典kv
     * @param fType 字典类型
     * @return java.util.Map<java.lang.String, java.lang.Object>
     */
    public static Map<String, Object> getDircByType(String fType){
        return DIC_INFO.get(fType);
    }

    /**
     * @Description 刷新字典表
     * @param
     * @return void
     */
    public static void fresh(){
        tbDircList = tbDircMapper.selectAll();
        //.stream().filter(tbDirc -> FIELDNAME_FTYPE_MAP.containsValue(tbDirc.getFType())).collect(Collectors.toList()); //字典表全表扫描
        Set<String> allDircType = tbDircList.stream().filter(item->item!=null).map(TbDirc::getFType).collect(Collectors.toSet()); //获取字典类型set
        //根据字典类型组成相应kv
        allDircType.stream().filter(item->item!=null&&FIELDNAME_FTYPE_MAP.containsValue(item)).forEach(item->{
            //List<Map<String, Object>> l2 = tbDircMapper.selectDicInfoByType(item.toString());
            List<Map<String, Object>> l2 = selectDicInfoByType(item.toString());
            if(l2.isEmpty()){
                //log.warn("字典翻译工具类#请确定DB中有无"+item.toString()+"类型的数据！"+l2.toString());
                return;//同continue
            }
            DircConvertUtils.loadDircByColType(l2, item.toString());//初始化字典
            //DircConvertUtils.loadDicInfoByColTypeOverturn(l2, item.toString());//初始化反转字典
        });
    }

    /**
     * @Description 初始化工具类
     * @param
     * @return
     */
    public DircConvertUtils() {
        //实体成员变量名与数据表类型映射关系
        //此映射关系适用于非标签翻译
        FIELDNAME_FTYPE_MAP.put("popu_type", "10101");//人口类型
        FIELDNAME_FTYPE_MAP.put("popuType", "10101");//人口类型
        FIELDNAME_FTYPE_MAP.put("sex", "10102");//性别
        FIELDNAME_FTYPE_MAP.put("marital_status", "10104");//婚姻状况
        FIELDNAME_FTYPE_MAP.put("maritalStatus", "10104");//婚姻状况
        FIELDNAME_FTYPE_MAP.put("religion", "10107");//宗教信仰
        FIELDNAME_FTYPE_MAP.put("careerType", "10108");//职业类型
        FIELDNAME_FTYPE_MAP.put("career_type", "10108");//职业类型
        FIELDNAME_FTYPE_MAP.put("house_type", "10309");//房屋地址类型
        FIELDNAME_FTYPE_MAP.put("houseType", "10309");//房屋地址类型

        //从yml文件中加载字段 字典类型映射关系
        Yaml yaml = new Yaml();
        Map<String, Map<String, String>> mapMap =  (Map<String, Map<String,String>>) yaml.load(this
                .getClass().getClassLoader().getResourceAsStream(YAML_NAME));
        SCOPE_FIELDNAME_FTYPE_MAP.putAll(mapMap);
    }

    /**
     * @Description 将某个类型ｋｖ方法字典表中
     * @param _dicInfo 从数据库查询的字典信息
     * @param colType 字典信息对应的字典类型
     * @return void
     */
    private  static void loadDircByColType(List<Map<String, Object>> _dicInfo,String colType){
        Map<String, Object> colTypeKV = DIC_INFO.get(colType); // 获取以当前分类为键的map
        if (colTypeKV != null) {
            for (Map<String, Object> map : _dicInfo){
                //从这个类型的字典中取值放入大字典里
                colTypeKV.put((String) map.get(KEY_NAME), map.get(VALUE_NAME));
            }
            DIC_INFO.put(colType,colTypeKV);
        }else {
            //从这个类型的字典中取值放入大字典里
            colTypeKV = new ConcurrentHashMap<>();
            for (Map<String, Object> map : _dicInfo){
                colTypeKV.put((String) map.get(KEY_NAME), map.get(VALUE_NAME));
            }
            DIC_INFO.put(colType,colTypeKV);
        }
    }

    /**
     * @Description 初始化反转字典表
     * @param _dicInfo 字典表信息
     * @param colType 字典类型
     * @return void
     */
    private static void loadDicInfoByColTypeOverturn(List<Map<String, Object>> _dicInfo,String colType) {
        Map<String, Object> colTypeKV = DIC_INFO_OVERTURN.get(colType); // 获取以当前分类为键的map
        if (colTypeKV != null) {
            for (Map<String, Object> map : _dicInfo){
                //从这个类型的字典中取值放入反转字典里
                colTypeKV.put((String) map.get(VALUE_NAME), map.get(KEY_NAME));//以值为键，以code为值
            }
            DIC_INFO_OVERTURN.put(colType,colTypeKV);
        }else {
            //从这个类型的字典中取值放入反转字典里
            colTypeKV = new ConcurrentHashMap<>();
            for (Map<String, Object> map : _dicInfo){
                colTypeKV.put((String) map.get(VALUE_NAME), map.get(KEY_NAME));
            }
            DIC_INFO_OVERTURN.put(colType,colTypeKV);
        }
    }

    /**
     * @Description 翻译业务数据中的字典字段数据
     * @param result 拦截器拦截的结果
     * @param ifOverturn 是否使用反转字典？ true：中文翻译成编码  false：编码转成中文
     * @param scope 作用域
     * @return void
     */
    public static void convertDicInfo(List<Object> result,boolean ifOverturn,String scope) {
        if(DIC_INFO.isEmpty()){
            log.warn("字典翻译工具类#当前字典信息为空！无法翻译！");
            return;
        }
        if(!SCOPE_FIELDNAME_FTYPE_MAP.containsKey(scope)){
            log.warn("scope 不存在！");
            return;
        }
        for (Object res : result) {
            if(res==null) continue;
            if (res instanceof Map) { //map类型数据
                DircConvertUtils.convertDicColumnInfo4Map((Map) res,ifOverturn,scope);
            } else if (isBaseType(res.getClass().getTypeName()) == null) { //非基本类型
                DircConvertUtils.convertDicColumnInfo(res,ifOverturn,scope);
            } else {
                log.warn("当前结果集类型为基本类型 不翻译！");
                continue;//
            }
        }
    }

    /**
     * @Description 翻译业务数据中的字典字段数据
     * @param result 拦截器拦截的结果集
     * @param ifOverturn 是否使用反转字典？ true：中文翻译成编码  false：编码转成中文
     * @return void
     */
    public static void convertDicInfo(List<Object> result,boolean ifOverturn) {
        if(DIC_INFO.isEmpty()){
            log.warn("字典翻译工具类#当前字典信息为空！无法翻译！");
            return;
        }
        for (Object res : result) {
            if(res==null) continue;
            if (res instanceof Map) { //map类型数据
                DircConvertUtils.convertDicColumnInfo4Map((Map) res,ifOverturn);
            } else if (isBaseType(res.getClass().getTypeName()) == null) { //非基本类型
                DircConvertUtils.convertDicColumnInfo(res,ifOverturn);
            } else {
                log.warn("当前结果集类型为基本类型 不翻译！");
                continue;
            }
        }
    }
    /**
     * @Description 递归翻译
     * @param _value 当前数据中的子集
     * @param ifOverturn 是否使用反转字典？ true：中文翻译成编码  false：编码转成中文
     * @return boolean
     */
    private static boolean recursion(Object _value,boolean ifOverturn) {
        if (_value == null) return true;
        if (_value instanceof Map) {
            convertDicColumnInfo4Map((Map) _value,ifOverturn);
            return true;
        } else if (_value instanceof List) {
            convertDicInfo((List) _value,ifOverturn);
            return true;
        }
        return false;
    }

    /**
     * @Description 递归翻译
     * @param _value 当前数据中的子集
     * @param ifOverturn 是否使用反转字典？ true：中文翻译成编码  false：编码转成中文
     * @param scope  域
     * @return boolean
     */
    private static boolean recursion(Object _value,boolean ifOverturn,String scope) {
        if (_value == null) return true;
        if (_value instanceof Map) {
            convertDicColumnInfo4Map((Map) _value,ifOverturn,scope);
            return true;
        } else if (_value instanceof List) {
            convertDicInfo((List) _value,ifOverturn,scope);
            return true;
        }
        return false;
    }


    /**
     * @Description 翻译标签数据
     * @param labelTreeCode
     * @param result
     * @param ifOverturn 是否使用反转字典？ true：中文翻译成编码  false：编码转成中文
     * @return void
     */
    public  static void convertLabelMap(String labelTreeCode , Map<String, Object> result,boolean ifOverturn) {

        for (String key : result.keySet()) {
                if(recursion(result.get(key),ifOverturn)) continue;
                Map<String,String > labelFieldDircCodeMap = LABELROWID_FIELDNAME_FTYPE_MAP.get(labelTreeCode);//像根据标签id拿到map
                String colType = labelFieldDircCodeMap.get(key);//再根据结果集中的key 找到类型
                Map<String, Object> colTypeKV = null; //有了类型，找到字典类型
                if(colType!=null){//如果当前查询结果集中有需要翻译的类型就去翻译。
                    colType = colType.replace("\r\n", "");
                    if(!ifOverturn) colTypeKV = DIC_INFO.get(colType);//如果ifOverTurn为false,则不使用反转
                    if(ifOverturn) colTypeKV = DIC_INFO_OVERTURN.get(colType);
                    if (colTypeKV != null) {//拿到类型字典的kv
                        String biz_value = result.get(key).toString(); //拿到结果集的value
                        result.put(key, colTypeKV.get(biz_value)); //设置新的value
                    }
                }else{
                    log.warn("字典翻译工具类#当前标签Map中的[{}]不能被翻译",key);
                    continue;
                }
        }

    }


    /**
     * @Description 翻译map类数据
     * @param result
     * @param ifOverturn 是否使用反转字典？ true：中文翻译成编码  false：编码转成中文
     * @param scope 作用域
     * @return void
     */
    private static void convertDicColumnInfo4Map(Map<String, Object> result,boolean ifOverturn,String scope) {
        log.info("字典翻译工具类#当前结果集类型为Map");
        result.keySet().forEach(key -> {//查询结果集
            if(recursion(result.get(key),ifOverturn,scope)) return;
            //根据返回结果集去定位类型
            Map<String, String> FIELDNAME_FTYPE_MAP_TEMP = SCOPE_FIELDNAME_FTYPE_MAP.get(scope);
            String colType = FIELDNAME_FTYPE_MAP_TEMP.get(key);
            commonMethod4Map(result, ifOverturn, key, colType);
        });
    }


    /**
     * @Description 翻译map类数据
     * @param result
     * @param ifOverturn 是否使用反转字典？ true：中文翻译成编码  false：编码转成中文
     * @return void
     */
    private static void convertDicColumnInfo4Map(Map<String, Object> result,boolean ifOverturn) {
        log.info("字典翻译工具类#当前结果集类型为Map");
        result.keySet().forEach(key -> {//查询结果集
            if(recursion(result.get(key),ifOverturn)) return;
            //根据返回结果集去定位类型
            String colType = FIELDNAME_FTYPE_MAP.get(key);
            commonMethod4Map(result, ifOverturn, key, colType);
        });
    }


    /**
     * @Description 翻译实体类数据
     * @param result
     * @param ifOverturn 是否使用反转字典？ true：中文翻译成编码  false：编码转成中文
     * @param scope 作用域
     * @return void
     */
    private static void convertDicColumnInfo(Object result,boolean ifOverturn,String scope) {
        List<Field> allFields = Arrays.asList(result.getClass().getDeclaredFields());
        if(allFields.size()==0){
            return;
        }
        allFields.forEach(field -> {
            try {
                //指定类名和字段名就能找到该字段的get方法
                Method getMethod = getMethod(result.getClass(), field.getName());
                //根据方法返回值判断是否需要递归进行翻译
                Object value = null;
                try{
                    value = getMethod.invoke(result);
                    if(recursion(value,ifOverturn,scope)) return;
                }catch (RuntimeException e){
                    return;//翻译下一个
                }

                //根据当前的实体属性名去map里面去查找
                String colType =FIELDNAME_FTYPE_MAP.get(field.getName()) ;//迭代实体类的所有字段判断是否有可翻译的类型
                Map<String, Object> colTypeKV = null;//如果这个字段的类型存在就翻译
                if(!ifOverturn) colTypeKV = DIC_INFO.get(colType);//如果ifOverTurn为false,则不使用反转
                if(ifOverturn) colTypeKV = DIC_INFO_OVERTURN.get(colType);
                //log.info("字典翻译工具类#当前翻译字段名为" + field.getName() + "字段名对应的类型名为" + colType + "类型对应的字典为：" + colTypeKV);
                if (colTypeKV != null) {
                    //获取实体类的set方法
                    Method setMethod = setMethod(result.getClass(), field.getName(), String.class);
                    //执行调用
                    setMethod.invoke(result, colTypeKV.get(value));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }


    /**
     * @Description 翻译实体类数据
     * @param result
     * @param ifOverturn 是否使用反转字典？ true：中文翻译成编码  false：编码转成中文
     * @return void
     */
    private static void convertDicColumnInfo(Object result,boolean ifOverturn) {
        log.info("字典翻译工具类#翻译实体类数据：[{}]",result);
        List<Field> allFields = Arrays.asList(result.getClass().getDeclaredFields());
        if(allFields.size()==0){
            return;
        }
        allFields.forEach(field -> {
            try {
                //指定类名和字段名就能找到该字段的get方法
                Method getMethod = getMethod(result.getClass(), field.getName());
                //根据方法返回值判断是否需要递归进行翻译
                Object value = null;
                try{
                    value = getMethod.invoke(result);
                    if(recursion(value,ifOverturn)) return;
                }catch (RuntimeException e){
                    log.warn("[{}]没有这个方法",field.getName());
                    return;//翻译下一个
                }
                //根据当前的实体属性名去map里面去查找
                String colType =FIELDNAME_FTYPE_MAP.get(field.getName()) ;//迭代实体类的所有字段判断是否有可翻译的类型
                Map<String, Object> colTypeKV = null;//如果这个字段的类型存在就翻译
                if(!ifOverturn) colTypeKV = DIC_INFO.get(colType);//如果ifOverTurn为false,则不使用反转
                if(ifOverturn) colTypeKV = DIC_INFO_OVERTURN.get(colType);
                //log.info("字典翻译工具类#当前翻译字段名为" + field.getName() + "字段名对应的类型名为" + colType + "类型对应的字典为：" + colTypeKV);
                if (colTypeKV != null) {
                    //获取实体类的set方法
                    Method setMethod = setMethod(result.getClass(), field.getName(), String.class);
                    //执行调用
                    setMethod.invoke(result, colTypeKV.get(value));
                }
            } catch (Exception ignored) {}
        });
    }



    //==================================================================================
    //==============================================通用方法============================
    //==================================================================================


    /**
     * @Description 使用反射获取某个class的所有字段
     * @param className
     * @return java.util.List<java.lang.reflect.Field>
     */
    private static  List<Field> getAllFields(Class<?> className) {
        return Arrays.asList(className.getFields());
    }

    /**
     * @Description 使用反射获取某个class的get方法
     * @param className
     * @param fieldName
     * @return java.lang.reflect.Method
     */
    private static  Method getMethod(Class<?> className,String fieldName) {
        try {
            //获取get方法前，先进行字符串拼接
            StringBuilder methodName = new StringBuilder("get");
            methodName.append(captureName(fieldName));
            Method method = className.getMethod(methodName.toString());
            return method;
        } catch (NoSuchMethodException e) {
            log.warn("无此方法");
            return null;
        }
    }

    /**
     * @Description 首字母转大写
     * @param name
     * @return java.lang.String
     */
    private static String captureName(String name) {
        char[] cs=name.toCharArray();
        if(cs[0]>=65&&cs[0]<=90){
            //如果本来就是大写字母即刻返回
            return String.valueOf(cs);
        }
        //首字符默认是字母
        cs[0]-=32;
        return String.valueOf(cs);
    }

    /**
     * @Description 使用反射获取某个class的set方法
     * @param className
     * @param fieldName
     * @param paraType
     * @return java.lang.reflect.Method
     */
    private static  Method setMethod(Class<?> className,String fieldName,Class paraType) {
        try {
            //首字母大写后进行字符串拼接 setPopuType(String popuType)
            StringBuilder getMethodName = new StringBuilder("set");
            getMethodName.append(captureName(fieldName));
            Method setMethod  = className.getMethod(getMethodName.toString(), paraType);
            return setMethod;
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
            return null;
        }
    }


    /**
     * @Description 判断字段类型是否为基本数据类型
     * @param typeName
     * @return java.lang.Class<?>
     */
    private static Class<?> isBaseType(String typeName) {
        final Object[][] baseTypes = {{"byte", byte.class}, {"short", short.class}, {"char", char.class}, {"int", int.class}, {"java.lang.Long", long.class},{"Long", Long.class}, {"float", float.class}, {"double", double.class}, {"boolean", boolean.class}};
        for (int i = 0; i < baseTypes.length; i = i + 1) {
            Object[] type = baseTypes[i];
            if (type[0].equals(typeName)) {
                return (Class<?>)type[1];
            }
        }
        return null; //当前类型不是基本类型返回null
    }

    private static void commonMethod4Map(Map<String, Object> result, boolean ifOverturn, String key, String colType) {
        Map<String, Object> colTypeKV = null;
        if (colType != null) {//如果当前查询结果集中有需要翻译的类型就去翻译。
            if (!ifOverturn) colTypeKV = DIC_INFO.get(colType);//如果ifOverTurn为false,则不使用反转
            if (ifOverturn) colTypeKV = DIC_INFO_OVERTURN.get(colType);
            if (colTypeKV != null) {
                Object biz_value = result.get(key);
                result.put(key, colTypeKV.get(biz_value));
            }
        } else {
            log.info("字典翻译工具类#当前map中的 [{}]不能被翻译",key);
            return;
        }
    }

    /**
     * @Description 根据字典类型查询字典详情
     * @param type 字典类型
     * @return java.util.List<java.util.Map < java.lang.String, java.lang.Object>>
     */
    private static List<Map<String, Object>> selectDicInfoByType(String type) {
        List<Map<String, Object>> mapList = new ArrayList<>();
        tbDircList.stream().filter(tbDirc -> tbDirc.getFType().equals(type))
                .forEach(tbDirc -> {
                    HashMap<String, Object> hashMapTemp = new HashMap<>();
                    hashMapTemp.put(KEY_NAME,tbDirc.getFKey());
                    hashMapTemp.put(VALUE_NAME, tbDirc.getFValue());
                    mapList.add(hashMapTemp);
                });
        return mapList;
    }
}
