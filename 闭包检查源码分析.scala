闭包检查源码分析
序列化时，会进行闭包检查，如果是闭包情况下，
Driver端的数据到Exeutor以及最后Executor的数据进行回收给Driver展示，
会存在闭包的问题。
内部的函数会访问到外部的属性。
例如：

//对象在Driver端创建 rdd
//rdd 执行foreach ==>遍历会触发不同分区的数据，交给不同的Executor执行 ，数据会从Driver到Executor进行“传递”
val rdd: RDD[Student] = sc.makeRDD(List(std1,std2))
rdd.collect().foreach(a=>println(a))
//内部函数访问了外部变量  --->闭包了

rdd.foreach(a=>println(a))
 def foreach(f: T => Unit): Unit = withScope {
    val cleanF = sc.clean(f)
    sc.runJob(this, (iter: Iterator[T]) => iter.foreach(cleanF))
  }


 private[spark] def clean[F <: AnyRef](f: F, checkSerializable: Boolean = true): F = {
    ClosureCleaner.clean(f, checkSerializable)
    f
  }

最深层的clean函数会进行闭包检查
    if (!isClosure(func.getClass)) {
      logWarning("Expected a closure; got " + func.getClass.getName)
      return
    }

   // Check whether a class represents a Scala closure
  private def isClosure(cls: Class[_]): Boolean = {
    cls.getName.contains("$anonfun$")  //从底层检查是否为闭包
  }   

  如果不是闭包，直接跳出来 ，不用进行检查
  是闭包进行下一步

  //下一步检查序列化
  if (checkSerializable) {
  	ensureSerializable(func)
    }
  }

    private def ensureSerializable(func: AnyRef) {
    try {
      if (SparkEnv.get != null) {
        SparkEnv.get.closureSerializer.newInstance().serialize(func)
      }
    } catch {
      case ex: Exception => throw new SparkException("Task not serializable", ex)
    }
  }

seriallize -->抽象类
具体看是由什么实现的
SerializerInstance (org.apache.spark.serializer)
DummySerializerInstance (org.apache.spark.serializer)
JavaSerializerInstance (org.apache.spark.serializer)
KryoSerializerInstance (org.apache.spark.serializer)
通过看JavaSerialize...的实现
  override def serialize[T: ClassTag](t: T): ByteBuffer = {
    val bos = new ByteBufferOutputStream()
    val out = serializeStream(bos)
    out.writeObject(t)
    out.close()
    bos.toByteBuffer
  }

  最后找到JavaSerializationStream类下
  最后在ObjectOutputStream下
 private void writeObject0(Object obj, boolean unshared){
       if (obj instanceof String) {
                writeString((String) obj, unshared);
            } else if (cl.isArray()) {
                writeArray(obj, desc, unshared);
            } else if (obj instanceof Enum) {
                writeEnum((Enum<?>) obj, desc, unshared);
            } else if (obj instanceof Serializable) {
                writeOrdinaryObject(obj, desc, unshared);
            } else {
            		......
            		......
         else {
                    throw new NotSerializableException(cl.getName());
                }   
        }
  判断是否序列化，如果有，程序继续执行，
  如果没有序列化，抛出异常