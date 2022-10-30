(ns beetleman.rehydrate.async.core-async
  (:require [beetleman.rehydrate.async.proto :as proto]
            [clojure.core.async :as a]
            [promesa.core :as p])
  (:import [clojure.core.async.impl.channels  ManyToManyChannel]))


(extend-protocol proto/AsyncResoult
  ManyToManyChannel
  (into-future [this]
    (p/create (fn [resolve reject]
                (a/go
                  (try
                    (resolve (a/<! this))
                    (catch Exception e
                      (reject e))))))))
