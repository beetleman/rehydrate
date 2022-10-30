(ns beetleman.rehydrate.async.manifold
  (:require [manifold.deferred :as d]
            [promesa.core :as p]
            [beetleman.rehydrate.async.proto :as proto])
  (:import [manifold.deferred Deferred]))


(extend-protocol proto/AsyncResoult
  Deferred
  (into-future [this]
    (p/create (fn [resolve reject]
                (d/on-realized this resolve reject)))))
