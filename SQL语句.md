# SQL语句-DDL&DML

## 一、DDL 数据库定义语言，主要处理数据库对象

### **DDL 数据库定义语言，主要处理数据库对象**

**create** 

**show** 

**alter**

**drop**

```sql
----DDL 数据库定义语言，主要处理数据库对象
--- create show alter drop


--创建数据库
create database if not exists 数据库名称 charset utf8;
--查看库或者表
show create table 表名称 -- 查看表的结构
show tables -- 查看库中的表
desc 表明  -- 查看表
--修改数据库
alter database 数据库名称 charset 新的字符集;
--丢弃数据库
drop database if exists 数据库名称
```

----



### *create 建表方式

##### 	1>在数据库下建一个全新的表

```sql
create table if not exists 表名(
	列1名称 数据类型1(长度) 其他选项
    列2名称 数据类型2(长度) 其他选项
    ............
    --主键
    primary key(某一列,一般是auto_increment)
)engine 数据库引擎 charset 字符集

--note1:数据库引擎
--InnoDB : 缺省引擎,支持事物,外键等高级特性
--MyIsam : 速度快,不支持事物,外键等高级特性

--note2:其他选项
--auto_increment,自增
--default , 默认缺省值
--not null , 不能为空
--unique ,唯一 不能重复


--exercise //构建一个teacher表
create table if not exists teacher(
	id int auto_increment,
    name varchar(20),
    age int,
    gender varchar(10),
    address varchar(20),
    primary key(id)
)engine InnoDB charset gbk;


```

---

##### 2>基于子查询，不能复制各种约束

```sql
--子查询
create table if not exists 新表名称 子数组
--例如：
create table if not exists students2 select * from students;
```

![](F:\上课\sql查询子表建表.png)

​	

![](F:\上课\子查询建表与原表对比.png)

##### 3>可以完全复制表结构的建表

```sql
create table if not exists 新表名 like 已有的表名
--例如
create table if not exists students3 like students; 
```

![](F:\上课\完全复制表结构.png)

----



### *alter 表结构的修改

```sql
alter table 表名称
--子句

--添加新列结构
alter table 表名称
add 新列名 数据类型 其他选项
--如
alter table	students2
add phone int defalut 187 ;

--修改已有列结构
alter table 表名称
modify 列名 新数据类型 新其他选项
--如
alter table students2
modify phone varchar(11) default '187';

--修改列名
alter table 表名称
change 老列名 新列名 新的数据类型 新的其他选项
--如
alter table students2
change phone email varchar(11) default '187@';

--丢弃一列
alter table 表名称
drop column 列名
--如
alter table students2
drop column email;

--修改表名
alter table 表名称
rename to 新名称
--如
alter table students2
rename to studentQq;
```

![](F:\上课\表结构的修改1.png)

![](F:\上课\表结构修改2.png)

![](F:\上课\表结构修改3.png)

### *show drop 上述中有体现



---

## 二、DML数据库操纵语言

#### DML针对表中的数据进行操作,这样的语言称作DML

#### *select     R

查询很难,哈哈哈哈哈.........

可以在网上找些练习来做。

```sql
--这里是简单介绍查询所使用的关键字以及写的位置
select 
from
join 
on
where
group by 
having 
order by 
--下面是执行时的顺序
from --确定基表
join --基表数据不够时,联接其他表
on --确定联接是的基准,以... 联接
where --过滤条件 ,  过滤基表中的一行
group by --分组
select --遍历
having --进一步的根据数据来过滤
order by --排序  默认升序 / +desc降序


--NOTE:!
-- 在起别名时,一定要按照执行顺序来起,要不然会找不到到底是什么！！！
```



#### *update   U

通用的跟新操作，修改数据值什么的

```sql
update 表名称
	列1 = 值1，
	列2 = 值2，
	列3 = 值3
where --进行行过滤
--如
update students set
	name = "二狗",
	grade = " 十年级",
	gender = "女"
where id = 5;	
```

![](F:\上课\修改前.png)

![](F:\上课\修改后.png)

#### *delete    D

```sql
--删除，可以进行回滚
delete from 表名称
where 进行过滤
--如
delete from studentqq--前面改过名称了
where id > 1; --过滤
```



#### *insert     C

##### 1>简单插入

```sql
insert into students(
	name,
    grade,
    gender,
    score
) values(
	"佳佳",
    "八年级",
    "男",
    100
),(
	"伟哥",
    "二年级",
    "男",
    72
);
```

##### 2> 使用子表插入

```sql
insert into students3( --好像表结构不对,插不进去,反正理解就是这个味儿就行
	name,
    grade,
    gender,
    score
) select (
	name,
    grade,
    gender,
    score
from 
    students
where id in (3,4)
);

```

##### 3>复制表并且同时插入数据

```sql
create table 新表 like 旧表
insert into 新表 select * from 旧表
```

##### 4>插入数据

```sql
insert into 表名称
set  
	列1 = 值1，
	列2 = 值2，
	列3 = 值3；	
```

----

## 

## 三、预编译

```sql
prepare p from 'sql'; --sql中有?
--如
prepare p from 
	'select * from students';
	
execute p; 执行预编译	


--预编译插入数据
prepare p1 from 
	'insert into students(
		name,
		grade,
		gender,
		score
	) values (
		?,
		?,
		?,
		?
	)';


set @name = '花花',@grade = '二年级' ,@gender = '女',@score = 83;

execute p1 using @name,@grade,@gender,@score;
	
```



![](F:\上课\预编译.png)

---

## 四、五种约束

#### *NOT NULL 非空约束，规定某个字段不能为空

#### *UNIQUE 唯一，表中不能有重复的

#### *PRIMARY KEY 主键（一般由自增的充当）

#### *FOREIGN KEY 外键

#### *DEFAULT 默认值

#### 