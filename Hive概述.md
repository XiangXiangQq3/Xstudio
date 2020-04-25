## Hive

Hive是基于Hadoop的一个数据仓库工具，可以将结构化的数据文件映射为一张表，并且能够提供类Sql的查询功能。

1）Hive处理的数据是在HDFS上，

2）Hive分析数据底层的实现是MapReduce，

3）执行程序运行在Yarn上

注意一下元数据（Metastore）

元数据包含表名，表所属的数据库，表的拥有者，列/分区的字段，表的类型（是否为外部表），表的数据所在目录。

元数据默认存储在自带derby数据库，平常我们所用时使用Mysql存储Metastore

----

### 一、Hive的数据类型

【bit：位 ，是计算机中最小的单位，只有0和1，每个0或1就是一个bit

byte：字节 ，1个字节=8个比特 =一个字母=一个字节

1个汉字=2个字节=16个比特

1 Byte = 8 Bits

1 KB = 1024 Bytes

1 MB = 1024 KB

1 GB = 1024 MB】



#### 1>基本数据类型

TinyInt 、Smallint、Int、BigInt、Boolean、Float、Double、String、Binary、Timestamp

#### 2>集合数据类型

Array、Map、Struct

#### 3>类型转化

1.任何整数类型都可以隐式转化为一个更加广泛的类型，例如Tinyint可以转化位Int，Int可以转化为Bigint；

2.所有的整数类型，Float和String类型可以隐式的转化为Double

3.进行强类型转化时，要使用Cast，如果类型转化失败，则返回null

```sql
--例如
select "1"+2 , cast("1" as int)+2;
--结果是 3.0 和 3
```

----

### 二、DDL数据库定义语言

#### 1>创建数据库

```sql
create database "数据库名称"
[comment "数据库的描述"]
[location "hdfs的路径"]
[with dbproperties ("属性名称"="属性值")];

--例如
create database test_db location "/hive_db";
```

#### 2>查看数据库

```sql
--显示数据库
show databases;
--显示数据库详情
desc database [extended] "数据库名称";
--切换数据库
use "数据库名称"
--删除数据库
drop database "数据库名称" [cascade];
```

#### 3>创建表

```sql
create [external] table [if not exists] "表名称"
(
    "列名1" "数据类型int/string/..." [comment "列1描述"],
    "列名2" "数据类型int/string/..." [comment "列2描述"],
    ....
)
[comment "描述表"]
[partitioned by ("新列名" "数据类型") [comment "分区描述"]]
[clustered by ("列名") into "分桶数" buckets] --一般都是按照键进行分桶
[-- 例如 
row format delimited fields terminated by "\t"]
[location "hdfs路径"]
[stored as "存储的结构" tblproperties ("属性名称" = "属性值")]

--例如
create external table if not exists test
(
    id int , name string comment "is ..."
)
partitioned by (month string)
clustered by (id) 
into 4 buckets
row format delimited
fields terminated by "\t"
collection items terminated by ","
map keys terminated by "&"
lines terminated by "\n"
stored as orc tblproperties ("orc.compress" = "NONE");
--详细查看表结构
desc formatted test
--切换表的类型
set tblproperties ("external" = true/false)
```

#### 4>分区表的操作

```sql
--加载数据到分区表中
load data local inpath "/opt/module/datas/test.txt" into table partition(month = "2020")

--增加分区
alter table test add partition(month = "2021");

--删除分区
alter table test drop partition(month = "2020"),partition(month = "2021");

--查看分区表有多少个分区
show partitions test;

--如果把数据直接上传到hdfs上，需要执行修复命令
--例如
dfs -mkdir -p/user/hive/warehouse/test/month=2020;
dfs -put /opt/module/datas/test.txt /user/hive/warehouse/test/month=2020;
--修复
msck repair table test;
```

#### 5>修改表

```sql
--增加列
alter table test add columns (address string);
--跟新列
alter table test change columns address location string;
--替换列
alter table test replace columns (phone int ,name string ,loc string);
```

----

### 三、DML数据库操作语言

#### 1>加载数据

```sql
--加载文件
load data [local] inpath "opt/module/datas/test.txt" [overwrite] into table test [partition("分区名" = "分区量")];

--插入数据
insert into table test partition(month = "2020") values (1,"2"),(2,"3");
--通过查询
insert overwite table test1 partition(month = "2020") select id , name from test where month = "2020";

--创建表时也可以加入数据
create table test1 as select id ,name from test;

--清除表数据
truncate table test; --只能删出内部表 ， 不能删除外部表   外部数据删除 内部表结构还在但是数据为空
```

#### 2>排序

```sql
--设置reduce数量
set mapreduce.job.reduce = 3;
--局部排序
select * from test sort by id desc ; --默认按照这一整行的hash值对分区数取余进行排序
--指定分区排序的规则
select * from test distribute by sort by id desc ;
--cluster by
select * from tesr cluster by id; --只能是升序排列 相等于 distribute by sort by 的连用
```

#### 3>分区和分桶

分桶是将数据分成若干个文件

分区针对的是数据的存储路径

**注意：分桶进行导入数据时一定要借助第三方表格，直接导入是不会进行分桶的**

```sql
--分桶抽样查询
--所有数据都可以进行抽样，不一定非要是分桶

select * from test tablesample(bucket 1 out of 4 on id); --将数据分成四桶取第一份数据
```

#### 4>函数

1.空字段赋值NVL

```sql
select name,nvl(name , 888) from test ;
select *,nvl(name,id) from test ;
```

2.case when

```sql
select 
	id,
	sum(case name when "aa" then 1 else 0) aacount,
	sum(case when "bb" then 1 else 0) bbcount
from 
	test;
group id;	
```

3.concat_ws 

```sql
select 
	concat_ws ("|" , collect _set(name))
from test;
```

4.炸开类别

```sql
--1.explode
select 
	explode(name) names
from test;

--lateral view
select
	id,
	names
from 
	test
lateral view explode(name) tbl as names;	
```

#### 5>开窗函数

```sql
select 
	id,
	name,
	sum(1) over(partition by name order by id) s1
from test;	

---

select 
	id,
	name,
	sum(1) over(partition by name order by id rows between unbounded preceding and current now) s2
from test;
```

#### 6>Rank

```sql
90 90 86 85
rank() 1 1 3 4
dense_rank() 1 1 2 3
row_number() 1 2 3 4
```

----

**其他更细致的在：**

[https://blog.csdn.net/weixin_45943729/article/details/103939107]: 

