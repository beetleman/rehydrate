(ns beetleman.rehydrate.async
  (:require [promesa.core :as p])
  (:import [java.util.concurrent CompletableFuture]))


(defprotocol AsyncResoult
 (into-future [_]))


(extend-protocol AsyncResoult
 Object
 (into-future [this] (p/promise this))

 CompletableFuture
 (into-future [this] this)

 clojure.lang.IDeref
 (into-future [this] (p/future @this)))
