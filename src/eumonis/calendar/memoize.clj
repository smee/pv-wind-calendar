(ns ^{:author "Meikel Brandmeyer" :doc "see http://kotka.de/blog/2010/03/memoize_done_right.html"} 
     eumonis.calendar.memoize
  (:refer-clojure :exclude [memoize]))

(defprotocol PCachingStrategy
  "A caching strategy implements the backend for memoize. It handles the
  underlying cache and might define different strategies to remove „old“
  items from the cache."
  (retrieve [cache item] "Get the requested cache item.")
  (cached?  [cache item] "Checks whether the given argument list is cached.")
  (hit      [cache item] "Called in case of a cache hit.")
  (miss     [cache item result] "Called in case of a cache miss."))

(declare naive-cache-strategy)

(defn memoize
  "Returns a memoized version of a referentially transparent function.
  The memoized version of the function keeps a cache of the mapping from
  arguments to results and, when calls with the same arguments are repeated
  often, has higher performance at the expense of higher memory use.
  Optionally takes a cache strategy. Default is the naive safe all strategy."
  ([f] (memoize f (naive-cache-strategy)))
  ([f strategy]
   (let [cache-state (atom strategy)
         hit-or-miss (fn [cache item]
                       (if (cached? cache item)
                         (hit cache item)
                         (miss cache item (delay (apply f item)))))]
     (fn [& args]
       (let [cs (swap! cache-state hit-or-miss args)]
         @(retrieve cs args))))))

(deftype NaiveStrategy [cache]
  PCachingStrategy
  (retrieve
    [_ item]
    (get cache item))
  (cached?
    [_ item]
    (contains? cache item))
  (hit
    [this _]
    this)
  (miss
    [_ item result]
    (NaiveStrategy. (assoc cache item result))))

(defn naive-cache-strategy
  "The naive safe-all cache strategy for memoize."
  []
  (NaiveStrategy. {}))

(deftype FifoStrategy [cache queue]
  PCachingStrategy
  (retrieve
    [_ item]
    (get cache item))
  (cached?
    [_ item]
    (contains? cache item))
  (hit
    [this _]
    this)
  (miss
    [_ item result]
    (let [k (peek queue)]
      (FifoStrategy. (-> cache (dissoc k) (assoc item result))
                     (-> queue pop (conj item))))))

(defn fifo-cache-strategy
  "Implements a first-in-first-out cache strategy. When the given limit
  is reached, items from the cache will be dropped in insertion order."
  [limit]
  (FifoStrategy. {} (into clojure.lang.PersistentQueue/EMPTY
                          (repeat limit :dummy))))

(deftype LruStrategy [cache lru tick]
  PCachingStrategy
  (retrieve
    [_ item]
    (get cache item))
  (cached?
    [_ item]
    (contains? cache item))
  (hit
    [_ item]
    (let [tick (inc tick)]
      (LruStrategy. cache (assoc lru item tick) tick)))
  (miss
    [_ item result]
    (let [tick (inc tick)
          k    (apply min-key lru (keys lru))]
      (LruStrategy. (-> cache (dissoc k) (assoc item result))
                    (-> lru   (dissoc k) (assoc item tick))
                    tick))))

(defn lru-cache-strategy
  "Implements a LRU cache strategy, which drops the least recently used
  argument lists from the cache. If the given limit of items in the cache
  is reached, the longest unaccessed item is removed from the cache. In
  case there is a tie, the removal order is unspecified."
  [limit]
  (LruStrategy. {} (into {} (for [x (range (- limit) 0)] [x x])) 0))

(declare dissoc-dead)

(deftype TtlStrategy [cache ttl limit]
  PCachingStrategy
  (retrieve
    [_ item]
    (get cache item))
  (cached?
    [_ item]
    (when-let [t (get ttl item)]
      (< (- (System/currentTimeMillis) t) limit)))
  (hit
    [this _]
    this)
  (miss
    [this item result]
    (let [now  (System/currentTimeMillis)
          this (dissoc-dead this now)]
      (TtlStrategy. (assoc (:cache this) item result)
                    (assoc (:ttl this) item now)
                    limit))))

(defn- dissoc-dead
  [state now]
  (let [ks (map key (filter #(> (- now (val %)) (:limit state))
                            (:ttl state)))
        dissoc-ks #(apply dissoc % ks)]
    (TtlStrategy. (dissoc-ks (:cache state))
                  (dissoc-ks (:ttl state))
                  (:limit state))))

(defn ttl-cache-strategy
  "Implements a time-to-live cache strategy. Upon access to the cache
  all expired items will be removed. The time to live is defined by
  the given expiry time span. Items will only be removed on function
  call. Outdated items might be returned. No background activity is
  done."
  [limit]
  (TtlStrategy. {} {} limit))

(deftype LuStrategy [cache lu]
  PCachingStrategy
  (retrieve
    [_ item]
    (get cache item))
  (cached?
    [_ item]
    (contains? cache item))
  (hit
    [_ item]
    (LuStrategy. cache (update-in lu [item] inc)))
  (miss
    [_ item result]
    (let [k (apply min-key lu (keys lu))]
      (LuStrategy. (-> cache (dissoc k) (assoc item result))
                   (-> lu (dissoc k) (assoc item 0))))))

(defn lu-cache-strategy
  "Implements a least-used cache strategy. Upon access to the cache
  it will be tracked which items are requested. If the cache size reaches
  the given limit, items with the lowest usage count will be removed. In
  case of ties the removal order is unspecified."
  [limit]
  (LuStrategy. {} (into {} (for [x (range (- limit) 0)] [x x]))))