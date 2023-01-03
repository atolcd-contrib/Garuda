package cache

trait Cache[K,V] {
	private var cached: Map[K, V] = Map[K, V]()
	
	def add(key: K, value: V): Unit = {
		cached += key -> value
	}
	
	def remove(key: K): Unit = {
		cached -= key
	}
	
	def get(key: K): Option[V] = {
		cached.get(key)
	}
}
