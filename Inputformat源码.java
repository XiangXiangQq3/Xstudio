Hadoop Inputformat源码分析
//提交任务
boolean b = job.waitForCompletion(true);
调用waitForCompletion方法
在Job类（实现了JobContext接口）
确定任务状态
 if (state == JobState.DEFINE) {
      submit();
    }

---Submit the job to the cluster and return immediately.
在waitForCompletion方法里面调用submit方法
在sumbit()里面{
	1.再一次确定任务状态	
	2.调用connect{
		确定是本地集群还是Yarn集群
	}
	3.在sumbit里面将connect获取的集群信息赋值给submitter
		submitter在通过调用JobSubmitter类下的submitJobInternal（）方法将任务提交
			submitJobInternal（job，cluster）{
				checkSpecs(Job job){
					//检查job的outformat是否已经准备好，看路通不通
					output.checkOutputSpecs(job);
				}
				//生成job提交时的临时目录
				Path jobStagingArea = JobSubmissionFiles.getStagingDir(cluster, conf);
				//获取JobId
				JobID jobId = submitClient.getNewJobID();

				//根据临时目录和JobId获得真正的提交路径submitJobDi 
				//--提交完毕后，如果是集群会在集群上生成临时目录
				Path submitJobDir = new Path(jobStagingArea, jobId.toString());
				//然后向临时目录拷贝第一份文件，jar包
				//------AM会将jar包分发给各个节点
				copyAndConfigureFiles(job, submitJobDir);
				// Create the splits for the job
				//切片信息提交，没有真正切片，而是提交的切片信息
				int maps = writeSplits(job, submitJobDir);
 			 	conf.setInt(MRJobConfig.NUM_MAPS, maps);
 			 	// Write job file to submit dir
 			 	//提交配置信息
      			writeConf(conf, submitJobFile);
			}
}

切片方法
writeSplits(){
	//获取配置文件
	Configuration conf = job.getConfiguration();
    InputFormat<?, ?> input =
    //根据配置文件生成IputFormat实例，input  //InputFormat<?, ?> input是一个抽象父类，具体怎么切分还是看你子类怎么写，这里调用的是FileInputformat
    ReflectionUtils.newInstance(job.getInputFormatClass(), conf);
    //根据getSplits方法获取切片规划
    //FileInput可以看Spark切片
	Long


	//1、InputFormat除了进行切片
	//2、还进行了RecordReader

	--Driver端将文件切成许多份，对应每一个maptask， （maptask=splicts）
	--每一个maptask会调用Recordreader进行处理一个切片
	The record reader breaks the data into key/value pairs for input to the
	--Recordreader会将数据打碎成KV值 //比如说FileInputFormat是将数据打碎成一行一行的数据
	--KV值将被送往Mapper里面
	Mapper里面可以对KV进行逻辑上的处理

	//真正的切分是获取RecordReader
	RecordReader<K,V> createRecordReader(InputSplit split,
                                         TaskAttemptContext context
                                        ) throws IOException, 
                                                 InterruptedException
}