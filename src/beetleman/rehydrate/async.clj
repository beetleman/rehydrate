(ns beetleman.rehydrate.async
  (:require [beetleman.rehydrate.async.proto :as proto]
            [promesa.core :as p])
  (:import [java.util.concurrent CompletableFuture]))


(extend-protocol proto/AsyncResoult
 Object
 (into-future [this] (p/promise this))

 CompletableFuture
 (into-future [this] this)

 clojure.lang.IDeref
 (into-future [this] (p/future @this)))


(defn into-future [x]
  (proto/into-future x))
