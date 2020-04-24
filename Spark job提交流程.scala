Spark job提交流程

toDebugString 查看血缘关系
dependencies  查看依赖关系  返回的是上游Rdd与该Rdd的依赖关系  --- 宽依赖，窄依赖

1、Yarn 集群
	-运行环境
	-在一个运行环境中，可以运行多个应用
2、App应用
	-跑的一个程序，完成某个功能的代码（SparkContext）  // Application=SparkContext的个数
	-一个应用可以对应多个Job（每进行一次行动算子，都称为一次Job的提交）
3、Job
	-在编写的程序中，每次执行一次行动算子，都会触发一个Job
4、Stage
	-一个Job下对应多个Stage
	-Stage的数量 = 宽依赖的数量+1
5、Task
	-一个Stage下有许多Task
	-每个阶段最后一个RDD中的分区数，就是Task的数量（宽依赖出进行Stage划分）
	-task是在Executor上执行的单元
	Rdd处理的时候会用分区，实际上操作的是分区


在Yarn上
Spark应用提交（SparkContext），会向ResourceManger上提交（走Yarn任务调度），Yarn的RM会启动AM来推进任务，
AM会推进NM上先启动Driver线程，会在NodeManger启动ExecutorBackEnd，EBE类似于一个虚拟机，
Executor（执行器）会跑在上面进行算子操作，不能跨越多个节点，但是EBE上可以有多个Executor，task就跑在Executor里面
一个Executor上可以对Rdd的数据进行执行，当前执行时，分区对应相应的Task数。
一个Executor上可以跑这个RDD上的多个不同的分区，但是同一个分区的数据不能被多个Executor执行

---提交的流程
1、执行main方法-->初始化sc--->执行到action算子
2、DAGScheduler对Job进行切分Stage，Stage会产生task
3、TaskScheduler通过TaskSet获取Job的所有Task，然后序列化发往Executor


====================================================================
resultRDD.collect().foreach(println)
进入collect，不停的调用runJob函数，一直跟到runJob最底层
dagScheduler.runJob(rdd, cleanedFunc, partitions, callSite, resultHandler, localProperties.get)
在dagScheduler里面调用了runJob{
		.....
	 val waiter = submitJob(rdd, func, partitions, callSite, resultHandler, properties)
	 	.....
}

	submitJob{
		 eventProcessLoop.post(JobSubmitted(  //将Job封装成对象，通过Post传入过去
	      jobId, rdd, func2, partitions.toArray, callSite, waiter,
	      SerializationUtils.clone(properties)))
	}


		  def post(event: E): Unit = {
   			 eventQueue.put(event)  //将任务看成一个event封装进行队列里
  		}

  		在EventLoop类里面，封装有event的任务队列，开了一个线程去执行
  		 private val eventThread = new Thread(name) {  //开了一个线程执行run方法
 		   setDaemon(true)
			 override def run(): Unit = {
      	try {
        	while (!stopped.get) {
          	val event = eventQueue.take()  //将放入的队列的任务取出来
          	try {
            onReceive(event)
         	....
         	}
		}


		调用onReceive(抽象方法)==>找实现类-->找到DAGScheduler
		  override def onReceive(event: DAGSchedulerEvent): Unit = {
			    val timerContext = timer.time()
			    try {
			      doOnReceive(event)
			    } finally {
			      timerContext.stop()
		    }
 		 }


 		 		doOnReceiver{
 		 		 case JobSubmitted(jobId, rdd, func, partitions, callSite, listener, properties) =>
      				dagScheduler.handleJobSubmitted(jobId, rdd, func, partitions, callSite, listener, properties)  //真正的处理Job提交
      			........
 		 		}


 		 			handleJobSubmitted{//划分阶段
 		 				finalStage = createResultStage(finalRDD, func, partitions, jobId, callSite)
 		 				.....
 		 				.....
 		 				val job = new ActiveJob(jobId, finalStage, callSite, listener, properties)  //将阶段信息传入Job
 		 				....
		 				submitStage(finalStage)//提交阶段
 		 			}


 		 					createResultStage{
 		 							//提交job不管有没有宽依赖，都要创建一个阶段，（至少一个阶段）
 		 						val stage = new ResultStage(id, rdd, func, partitions, parents, jobId, callSite)
 		 						//如果有宽依赖，再创建一个阶段
		 						 val parents = getOrCreateParentStages(rdd, jobId)
 		 					}

 		 						getOrCreateParentStages{ 
 		 							   getShuffleDependencies(rdd).map { shuffleDep =>  //通过getShuffleDependencies获取宽依赖  //将宽依赖变成Stage
									      getOrCreateShuffleMapStage(shuffleDep, firstJobId)
									    }.toList
 		 						}	

 		 							//如何获取宽依赖
 		 							getShuffleDependencies{  // A <-- B <-- C   只能获取就进的依赖 ，C的依赖B
	 									 	 val parents = new HashSet[ShuffleDependency[_, _, _]]  //想要获取的依赖
									    	 val visited = new HashSet[RDD[_]] //以及处理过的
										     val waitingForVisit = new Stack[RDD[_]] //等待处理的
										    while (waitingForVisit.nonEmpty) {
										     遍历容器，然后进行模式匹配
										     到底是什么依赖
									     	case shuffleDep: ShuffleDependency[_, _, _] =>  //如果是宽依赖，放入集合中
									            parents += shuffleDep  //获取到parent的宽依赖
								          	case dependency =>  //如果不是宽依赖，放入栈中 //继续循环调用找上级依赖  //如果都没找到，集合就是空的，然后就不用new shuffle阶段了
            								waitingForVisit.push(dependency.rdd)
            								}

 		 							}



 		 								getOrCreateShuffleMapStage{ //如果有宽依赖，创建ShuffleMapStage
 		 										     if (!shuffleIdToMapStage.contains(dep.shuffleId)) {
												            createShuffleMapStage(dep, firstJobId)
												          }
 		 								}
 		 														createShuffleMapStage{
 		 															    val stage = new ShuffleMapStage(id, rdd, numTasks, parents, jobId, rdd.creationSite, shuffleDep)
 		 														}




 		 	 ------如果是这种关系  A-(宽)B    -C-（宽）D
 		 	 他会在底层调用 getMissingAncestorShuffleDependencies，底层会调类似于上面那种方法  //然后判断C的上层是否还要宽依赖
 		 	 getMissingAncestorShuffleDependencies{
 		 	 	      getShuffleDependencies(toVisit).foreach { shuffleDep =>
				          if (!shuffleIdToMapStage.contains(shuffleDep.shuffleId)) {
				            ancestors.push(shuffleDep)
				            waitingForVisit.push(shuffleDep.rdd)
				          } // Otherwise, the dependency and its ancestors have already been registered.
				        }
 		 	 }														










 		 	 -------------------------上面工作都做好了之后，会调用  submitStage(finalStage)

 		 	 submitStage{
 		 	 submitMissingTasks(stage, jobId.get)
 		 	 }

 		 	 	submitMissingTasks{
 		 	 		tasks: Seq[Task[_]](//进行模式匹配===>
 		 	 			stage match {
       					 case stage: ShuffleMapStage =>{
       					 	new ShuffleMapTask
       					 }
       					  case stage: ResultStage =>{
       					  	 new ResultTask(stage.id, stage.latestInfo.attemptId,
       					  }

 		 	 		)
 		 	 	}

 		 	 				......................
 							......................
 		 	 		if (tasks.size > 0) {  //如果是tasks集合大于0
 		 	 	   taskScheduler.submitTasks(new TaskSet( //这里是真正的提交任务tasks
       				tasks.toArray, stage.id, stage.latestInfo.attemptId, jobId, properties))
    				  stage.latestInfo.submissionTime = Some(clock.getTimeMillis())


    				  }
				  }









				  总结 ： 通过依赖关系找到到底创建宽依赖阶段封装给finalStage
				  		如果有宽依赖，创建shuffleMapStage 就会产生两个阶段 shuffleMapStage ， 和ResultStage 
				    	submitStage(finalStage) 提交阶段
				    通过模式匹配来根据创建的阶段来创建task，将task封装到tasks集合里面
				    	stage match {
       					 case stage: ShuffleMapStage =>{
       					 	new ShuffleMapTask  // 创建MapTask往磁盘写数据
       					 }
       					  case stage: ResultStage =>{
       					  	 new ResultTask(stage.id, stage.latestInfo.attemptId, //往磁盘读数据
       					  }


       					  1、创建的tasks集合，是怎么和分区建立联系的？
       					  2、foreachpartition？