# DircConvertUtil
字典翻译工具类，支持正向翻译，方向翻译，翻译map，翻译标签。
目前只支持Mybatis.


## 一、添加依赖
1、在 pom.xml中添加依赖：
```
<!--yaml jar包-->
<dependency>
    <groupId>org.yaml</groupId>
    <artifactId>snakeyaml</artifactId>
    <version>1.25</version>
</dependency>

<dependency>
    <groupId>org.projectlombok</groupId>
    <artifactId>lombok</artifactId>
</dependency>

<!--日志-->
<dependency>
    <groupId>org.slf4j</groupId>
    <artifactId>jcl-over-slf4j</artifactId>
</dependency>
``` 
2、控制台打印日志
```
mybatis:
  configuration:
    map-underscore-to-camel-case: true
    # 当查询数据为空时字段返回为null，不加这个查询数据为空时，字段将被隐藏
    call-setters-on-nulls: true
    # mybatis 在控制台打印sql日志
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl
  mapper-locations: classpath:mapper/*.xml
```
## 2、你需要提供什么接口
1、字典全表扫描接口 selectAll
```
  <select id="selectAll" resultMap="BaseResultMap">
    select row_id, f_type, f_key, f_value, parent_id, is_leaf, opt_status, opt_sort_by,
    opt_flag, create_by, create_time, update_by, update_time, remark
    from tb_dirc
  </select>
```
注意：目前你可将标签翻译的部分注掉
## 3、你需要修改的地方
1、把test.yml 替换成你的yml，注意此yml非application.yml
```
//yaml name
private static final String YAML_NAME = "test.yml";
```
2、将TbDircMapper TbLabelFieldMapper TbDirc分别替换成你的mapper和实体类。 目前可将TbLabelFieldMapper 注释掉。

3、在test.yml中添加自己的域
添加自己的域，添加待翻译的字段名和对应的字典类型。

![image](https://github.com/1500256797/images/20200424135632.png)
```
zzq: # 这是域
  popu_type: "10101"
  popuType: "10101"
  sex: "10102"
  religion :  "10107"
jack: # 这是域
  popu_type: "10101" # 翻译Map数据中的字段
  popuType: "10101"  #翻译实体中的字段
  sex: "10102"
  marital_status : "10104"
  religion :  "10107"
```
4、使用方式类型于pageHelper分页
```
    @GetMapping("/id")
    public Object getPopu() {
        ResultSetConvert.onWithFresh(2,"zzq"); //设置翻译次数 和 域
        //ResultSetConvert.onNoFresh(); //不同步、不设翻译次数；默认只翻译一次 不使用域 则会使用默认的FIELDNAME_FTYPE_MAP
        //ResultSetConvert.onWithFresh(); //只进行同步
        //ResultSetConvert.onWithFresh(1);//同步、并设翻译次数为1
        return popuService.selectPeopleByRowId("faf6e00b7aef11ea9c2c005056b17b89");
    }
```

## 4、更多
待完善。。。


## 5、特别鸣谢
1、ゝLibra `
