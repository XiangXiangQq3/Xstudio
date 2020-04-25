import java.io.Serializable;
import java.util.AbstractMap;
import java.util.Map;
import java.util.HashMap.Node;
import java.util.HashMap.TreeNode;

public class HashMap<K,V> extends AbstractMap<K,V> implements Map<K,V>, Cloneable, Serializable {
	
	// 常量 : 
	// 哈希表的缺省初始容量 : 16
    static final int DEFAULT_INITIAL_CAPACITY = 1 << 4; // aka 16

    // 最大容量
    static final int MAXIMUM_CAPACITY = 1 << 30;
    
    // 缺省的加载因子, 控制数组的使用比例, 到达这个比例后就扩容. 散列性提升.
    static final float DEFAULT_LOAD_FACTOR = 0.75f;
    
    // 冲突下标元素的个数超过这个值后, 就要变树.
    static final int TREEIFY_THRESHOLD = 8;
    
    // 冲突下标元素的个数小于这个值后, 把树变回链表
    static final int UNTREEIFY_THRESHOLD = 6;
    
    static class Node<K,V> implements Map.Entry<K,V> {
        final int hash; // 键对象的最原始的哈希值
        final K key; // 键对象
        V value; // 值对象
        Node<K,V> next; // 准备用作链表的.

        Node(int hash, K key, V value, Node<K,V> next) {
            this.hash = hash;
            this.key = key;
            this.value = value;
            this.next = next;
        }
    }
    
    // 最重要的哈希表, 保存元素
    transient Node<K,V>[] table;
    
    // 计数器
    transient int size;
    
    // 修改次数, 用于同步控制
    transient int modCount;
    
    // 当前哈希表的加载因子
    final float loadFactor;
    
    // 数组扩容门槛
    int threshold;

    public V put(K key, V value) {
        return putVal(hash(key), key, value, false, true);
    }
    
    static final int hash(Object key) {
        int h;
        return (key == null) ? 0 : (h = key.hashCode()) ^ (h >>> 16); // 提升散列性
    }
    
    // 第一个参数是键对象的原始哈希, 第二个参数是键对象, 第三个参数是值对象
    final V putVal(int hash, K key, V value, boolean onlyIfAbsent, boolean evict) {
		 Node<K,V>[] tab;  // tab就是哈希表
		 Node<K,V> p; // p是老元素
		 int n; // n是哈希表的长度
		 int i; // i是新键对象在数组中的目标下标.
		 tab = table;
		 n = tab.length; // 第一次不执行它
		 if (tab == null || n == 0) { // 第一次添加元素时会进入
			 tab = resize(); // 调整容量 
		     n = tab.length; // n是16
		 }
		 *****************************************************************************************************
		 // i就是键对象经过处理后的目标下标.
		 i = (n - 1) & hash; // 15 & 5, 相当于 hash % n
		 p = tab[i]; // 先取下标处的老元素. 如果有老元素说明下标有冲突, 如果没有老元素, 说明当前位置为空
		 if (p == null) {
		     tab[i] = newNode(hash, key, value, null); // 这是最好的结果, 条目直接插入.
		 ******************************************************************************************************
		 } else { // 下标有冲突, 一定会处理链表
		     Node<K,V> e; // 老元素
		     K k; // 老元素的键对象
		     if (p.hash == hash && // 老元素的原始哈希和新元素的原始哈希如果相等
		         ((k = p.key) == key|| // 老元素的键对象和新键对象是否是同一个对象, 如果是同一个对象, 直接不能插入
		         (key != null && key.equals(k)))) { // 老元素的键对象和新键对象equals为true, 也是键的冲突了
		         e = p;
		     } else if (p instanceof TreeNode) { // 如果老元素是一个TreeNode, 下标冲突位置处已经是一颗树了.
		         e = ((TreeNode<K,V>)p).putTreeVal(this, tab, hash, key, value); // 按照树的方式插入新元素
		     } else { // 链表式插入
		         for (int binCount = 0; ; ++binCount) { // binCount是用来给已有的链表元素计数的.
		             e = p.next; // 获取p元素的下一个结点对象
		        	 if (e == null) { // 如果为空, 说明p结点就是尾结点.
		                 p.next = newNode(hash, key, value, null); // 把新结点链入到尾结点后面.
		                 if (binCount >= TREEIFY_THRESHOLD - 1) { // 如果链表中的节点个数大于变树上限, 变树
			                 treeifyBin(tab, hash); // 把链表升级成红黑树
			                 break;
		        	 	 }
		        	 }
		        	 if (e.hash == hash &&
		        	    ((k = e.key) == key || 
		        	    (key != null && 
		        	    key.equals(k)))) { // 如果链表中的某节点的键对象 和 新键对象冲突了.
		        		 break;
		        	 }
		        	 
		        	 p = e; // 后移一个指针
		         }
		     }
		 
			 if (e != null) { // 当e不为空时, 说明键冲突了
		         V oldValue = e.value;
		         if (!onlyIfAbsent || oldValue == null) {
		             e.value = value; // 用新 值对象 替换 旧的值对象
		         }
		         afterNodeAccess(e);
		         return oldValue;
		     }
		 }
		 ++modCount; // 调整修改次数
		 if (++size > threshold) { // 调用计数器, 并作判断, 如果元素个数超过上限
		     resize(); // 调整容量
		 }
		 afterNodeInsertion(evict);
		 return null;
	}
    
    // 调整容量
    final Node<K,V>[] resize() {
        Node<K,V>[] oldTab = table; // oldTab是老表
        int oldCap = (oldTab == null) ? 0 : oldTab.length; // 老容量
        int oldThr = threshold; // 老上限
        int newCap = 0; 
        int newThr = 0;
        if (oldCap > 0) { // 第一次执行不进入, 扩容时进入
            if (oldCap >= MAXIMUM_CAPACITY) {
                threshold = Integer.MAX_VALUE;
                return oldTab;
            }
            else if ((newCap = oldCap << 1) < MAXIMUM_CAPACITY && oldCap >= DEFAULT_INITIAL_CAPACITY) { // 扩容, 容量翻番
                newThr = oldThr << 1; // 上限翻番
            }
        } else if (oldThr > 0) { // initial capacity was placed in threshold
            newCap = oldThr;
        } else { // 第一次添加元素时真正要进入的
            newCap = DEFAULT_INITIAL_CAPACITY; // 16
            newThr = (int)(DEFAULT_LOAD_FACTOR * DEFAULT_INITIAL_CAPACITY); // 12
        }
        if (newThr == 0) {
            float ft = (float)newCap * loadFactor;
            newThr = (newCap < MAXIMUM_CAPACITY && ft < (float)MAXIMUM_CAPACITY ?
                      (int)ft : Integer.MAX_VALUE);
        }
        
        threshold = newThr; // 当前上限修改为新上限
        @SuppressWarnings({"rawtypes","unchecked"})
        Node<K,V>[] newTab = (Node<K,V>[])new Node[newCap]; // 最重要的创建哈希表数组对象.
        table = newTab; // 当前哈希表就变成新的了
        if (oldTab != null) { // 第一次不会进入
            for (int j = 0; j < oldCap; ++j) {
                Node<K,V> e;
                if ((e = oldTab[j]) != null) {
                    oldTab[j] = null;
                    if (e.next == null)
                        newTab[e.hash & (newCap - 1)] = e;
                    else if (e instanceof TreeNode)
                        ((TreeNode<K,V>)e).split(this, newTab, j, oldCap);
                    else { // preserve order
                        Node<K,V> loHead = null, loTail = null;
                        Node<K,V> hiHead = null, hiTail = null;
                        Node<K,V> next;
                        do {
                            next = e.next;
                            if ((e.hash & oldCap) == 0) {
                                if (loTail == null)
                                    loHead = e;
                                else
                                    loTail.next = e;
                                loTail = e;
                            }
                            else {
                                if (hiTail == null)
                                    hiHead = e;
                                else
                                    hiTail.next = e;
                                hiTail = e;
                            }
                        } while ((e = next) != null);
                        if (loTail != null) {
                            loTail.next = null;
                            newTab[j] = loHead;
                        }
                        if (hiTail != null) {
                            hiTail.next = null;
                            newTab[j + oldCap] = hiHead;
                        }
                    }
                }
            }
        }
        return newTab;
    }

}