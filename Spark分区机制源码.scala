Spark分区机制源码分析
--从内存读取
sc.makeRDD(List(1,2,3,4))
	1>默认
		从内存的集合中读取数据创建RDD分区的数量取决于分配的CPU核数（Local[*]）


		sc.makeRDD(List(1,2,3,4))
		makeRDD底层调用了parallelize，如果没有传入参数，则默认numSlices为defaultParallelism
		（其中numSlices切片数=分区数，也同时意味着执行任务的时候开启Executor的数量是多少个）

		defaultParallelism是从TaskSchedulerlmpl类中的defaultParallelism()函数获取
		defaultParallelism()该函数在LocalScheduleBackend重写，调用了getInt方法获取默认值或者传入的值
		将该值记作totalcores。然后真正分区的时候是在SparkContext里面new LocalScheduleBackend传入totalcores构造器里
		进行模式匹配Local，然后获取到线程的数
		从而这样就指定了核数

		之后就可以调用parallelize函数进行实际上的分区
	2>指定
		调用parallelize函数，在parallelize里面new了一个ParallelCollectionRDD[T]
		在ParallelCollectionRDD[T]类里调用getPartitions函数，getPartitions里面调用
		slice函数，传入slice（data，numSlices）进行分区内数据的划分。
		（slice里进行的是模式匹配，匹配到数据比如是seq，将seq转化为数组）
		再调用 positions传入数组长度arr.length和切片numSlices数
		 positions内部进行相关算法操作
		 	length是arr的长度
			 val start = ((i * length) / numSlices).toInt
	       	 val end = (((i + 1) * length) / numSlices).toInt
	        (start, end)
	     -------------------
	     比如说5个元素 x，y，z，c，b 并 指定numSlices=3
	     通过算法计算
	     0 --> (0,1)  0位置取出   --------------------x
	     1 --> (1,3)  1，2位置取出 ------------------ y，z
	     2 --> (3,5)  3，4位置取出  ------------------c，b
	     -------------------   
	     将得到的（start，end）返回给positions进行arr.（start，end）.seq返回给分区





--从外部文件读取
//9000内部通信端口 rpc
sc.textFile("hdfs://hadoop105:9000/")
	1>默认
		用分配给应用的CPU核数和2取最小值
		sc.textFile("~")默认传入的是defaultMinPartitions
		def defaultMinPartitions: Int = math.min(defaultParallelism, 2)
	2>指定
		分区数的确定
 		* --textFile中，第二个参数实际上代表的是最小分区数minPartitions
		* --在实际分区时，会根据文件的字节大小和最小分区数minPartitions进行相除运算 ==>余数为0 那么最小分区就是实际的分区数
 		*    																	如果余数不为0的话，实际分区数应该大于最小分区数

 		分区里面内容的确定：
		textFile调用hadoopFile类，在hadoopFile类里new 了一个HadoopRDD
		通过调用getPartitions内调用inputformat获取切片
		inputFormat.getSplits(jobConf, minPartitions)
		inputFormat是个接口，底层这里通过调用FileInputformat实现功能
		在FileInputformat里面实现了getSplits(jobConf, minPartitions)
		//input是一个抽象父类，具体怎么切分还是看你子类怎么写，这里调用的是FileInputformat
		getSplits函数里有{ //Hadoop里面的FileInputFormat
		通过遍历文件夹-->累加字节计算 compute total size  total size字节大小
		//通过自己大小计算要每次切分的数量  gosize = 22
		long goalSize = totalSize / (numSplits == 0 ? 1 : numSplits);

		 // generate splits 通过集合产生切片规划
    	ArrayList<FileSplit> splits = new ArrayList<FileSplit>(numSplits);
    	对文件目录进行遍历
    	for (FileStatus file: files) {
    	找HDFS路径 blkLocation 0,22,localhost
    	找块大小	blockSize 33554432
    	 computeSplitSize{Math.max(minSize, Math.min(goalSize, blockSize));}
    	可以获取 每次切片大小 splitSize=7 
    	long bytesRemaining = length;
    	while (((double) bytesRemaining)/splitSize > SPLIT_SLOP //1.1) {
    	 bytesRemaining -= splitSize; //做切分操作
		}

		经过切片之后变为
		0 = {FileSplit@5295} "file:/D:/workspace_idea/bigDScala/Tspark/src/main/scala/com/xstudio/spark/input/Words2:0+7"
		1 = {FileSplit@5296} "file:/D:/workspace_idea/bigDScala/Tspark/src/main/scala/com/xstudio/spark/input/Words2:7+7"
		2 = {FileSplit@5297} "file:/D:/workspace_idea/bigDScala/Tspark/src/main/scala/com/xstudio/spark/input/Words2:14+7"
		3 = {FileSplit@5298} "file:/D:/workspace_idea/bigDScala/Tspark/src/main/scala/com/xstudio/spark/input/Words2:21+1"

		这样就拿到了切片规划，
		 val inputSplits = inputFormat.getSplits(jobConf, minPartitions)
		 val array = new Array[Partition](inputSplits.size)


		分区里的数据是怎么产生的呢？
		根据上述的切片规划并且
	
		是通过调用recordReader方法
		RecordReader<K,V> createRecordReader(InputSplit split,
                                         TaskAttemptContext context
                                        ) throws IOException, 
                                                 InterruptedException

		产生4个分区
		源数据
		0 1 2 3 4 5 6 7 8 
		a b c d e f g x x
		9 10 11 12 13 14
		h i  j  k  x  x
		15 16 17 18 19
		l  m  n  x  x
		20 21
		o  p

			 切片规划-->(start，end) -->结果
		结果	  始	取     
		p0 =>(0,7)--->(0,7)---->a b c d e f g x x
		p1 =>(7,7)--->(7,14)---->h i j k x x
		p2 =>(14,7)--->(14,21)---->l m n x x o p
		p3 =>(21,1)---->


		自己做个练习~
		源
		0  1  2  3  4  5  6  7  8  9  10 11
 		x  i  a  n  g  j  i  a  q  i  ~  ~
 		12 13 14 15 16 17 18 19 
		z  h  i  z  h  i  ~  ~
		20 21 22 23 24 25 26 27 28 29 30 31
		n  i  d  e  m  i  n  g  z  i  ~  ~
		32 33 34
		h  ~  ~
		35 36 37
		j  ~  ~
		38
		k 
		numSplice=4
		38/9 = 9
		38/9 = 4

		--->五个分区

		p0  ->(0,9)-->索引(0,9)          x i a n g j i a q i ~ ~
		p1  ->(9,9)-->索引(9,18)         z h i z h i ~ ~
		p2	->(18,9)-->索引(18,27)       n i d e m i n g z i ~ ~
		p3	->(27,9)-->索引(27,36)       h ~ ~ j ~ ~
		p4	->(36,9)-->索引(36,38)	     k	


