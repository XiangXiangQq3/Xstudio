## Java与Scala的集合对比

### 一、Java集合

面向对象语言对事物的体现是以对象的形式，为了对多个对象进行存储。单单靠数组不足以解决问题，同时对对象的操作极为的不方便。数组不可以存储不同的多个对象。

集合就像是一个容器，可以动态的把多个对象的引用放入到容器中。

**Collection集合:不按照添加的顺序存放对象的集合，集合内元素的内容是可以重复的。**

**保存一个一个的对象**

#### 1、Collection=>Set接口

​	元素不按照添加的顺序（无序）、不可重复添加相同元素（**内容而不是地址**）的集合

##### 	>HashSet

​		使用哈希算法实现的Set集合

​		**去重规则：两个对象的equals为true，并且两个对象的哈希码相等**

​		如果想让自定义对象重复，需要重写equals和hashCode

##### 	>LinkedSet 

##### 	>TreeSet

​		添加的顺序是无序的，且不可重复

​		**注意添加元素的时候不能添加不同的类型，因为会进行比较，不同类型的元素无法进行比较**

​		**1、自定义类要实现Comparable接口，实现并重写方法。**

​		**去重规则：compareTo返回0**

​		**2、写一个具体类，让这个类实现Comparator接口，重写compare方法，让比较器关联到TreeSet中**

​		使用树实现的Set集合，底层是通过二叉树实现的（=> **所以添加的数据，遍历出来后是看起来有顺序的**）

#### 2、Collection=>List接口	

元素按照添加的顺序（有序）、可重复添加相同元素的集合

##### 	>ArrayList

​		使用数组实现的List集合

##### 	>LinkedList

​		使用链表实现的List集合

##### 	>Vector

​		Vector：是线程安全的动态数组，底层是数组结构，初始化为长度为10的数组，如果容量满了，按照2.0倍扩容。除了支持foreach和Iterator遍历，还支持Enumeration迭代。

------

**ArrayList和LinkedList**

1.ArrayList是实现了基于动态数组的数据结构，LinkedList基于链表的数据结构。

2.对于随机访问get和set，ArrayList觉得优于LinkedList，因为LinkedList要移动指针。

3.对于新增和删除操作add和remove，LinedList比较占优势，因为ArrayList要移动数据。 这一点要看实际情况的。若只对单条数据插入或删除，ArrayList的速度反而优于LinkedList。但若是批量随机的插入删除数据，LinkedList的速度大大优于ArrayList. 因为ArrayList每插入一条数据，要移动插入点及之后的所有数据。

---

**Arraylist，LinkedList，Vector的区别**

ArrayList：是线程不安全的动态数组，底层是数组结构，JDK1.7后初始化为空数组，在添加第一个元素时初始化为长度为10的数组，如果容量满了，按照1.5倍扩容。支持foreach和Iterator遍历。

Vector：是线程安全的动态数组，底层是数组结构，初始化为长度为10的数组，如果容量满了，按照2.0倍扩容。除了支持foreach和Iterator遍历，还支持Enumeration迭代。

LinkedList：是双向链表，底层是链表结构。当频繁在集合中插入、删除元素时，效率较高，但是查找遍历的效率较低。

#### 3、Map接口

**Map集合：保存一对一对的对象**

具有映射关系“Key-Value”形式的集合

**1、Map中的key和value都可以是任何引用类型的数据**

**2、Map中的key是用set来进行存放的，不允许重复，也就是说同一个Map对象所对应的类，需要重写hashCode和equals方法**

**3、Map中的key和value存在单向一一对应关系，通过指定的key，可以唯一确定value的值**

Map是如何维护k-v的呢？

**Entry：横向来看，条目对象里面是一个一个的键值对，若干个Entry构成一个Map（无序不可重复）EntrySet**

**纵向来看KeySet专门放键，Collection放值**

##### 	>HashMap 

​		HashMap是线程不安全的哈希表，底层结构是JDK1.7时数组+链表，JDK1.8时数组+链表/红黑树。

HashMap的线程安全问题可以使用Collections的synchronizedMap(Map<K,V> m) 方法解决。

##### 	>TreeMap

##### 	> Hashtable

​		Hashtable是线程安全的哈希表，底层结构是数组+链表。

## 二、Scala集合

**>1、Scala集合有三个大类：序列Seq、集Set、映射Map。并且所有的集合都有自己扩展的特质**

**>2、对于几乎所有的集合类，Scala都同时提供了可变与不可变两个版本，位于两个包下**

​		不可变集合：scala.collection.immutable

​			不可变集合指的是，该集合的对象不能修改，每次修改过后，就会产生新的对象。这里修改指的是长度的改变，增加或减少。当只是修改对象里面的属性时，是可以的。

​		可变集合：scala.collection.immutable

​			可变集合指的是，可以对原对象修改，并且不会产生新的对象。

-----

常用  ==>

#### 1、Seq

不可变：**~**

-->IndexedSeq

**Array，String  ->底层隐式转化**

-->LinearSeq

**List，Queue，Stack**

可变：**~**

**ArrayBuffer**
**StringBuffer**

#### 2、Set

默认情况下，Set使用的是不可变集合，如果想要使用可变的集合，需要导包--scala.collection.mutable.Set

**无序，且数据不可重复**

#### 3、Map

创建Map，默认是不可改变的。

使用可变的时候，和Java的一样。

**值得注意的是：**

根据key，获取value值有两种情况~

1.获取到value

2.没有获取到，返回空

与java不同的是，Scala没有类似于Java直接获取（get()）方法，Scala为了避免取到null值，添加了新的类型Option

Option下有两个子类 None | Some  -- None相当于没获取到值，Some会对获取到的value进行包装处理
如果返回None，可以进行二次处理，给一个默认值

**如果真的想通过key来获取Value可以使用getOrElse(elem，default) 函数**

