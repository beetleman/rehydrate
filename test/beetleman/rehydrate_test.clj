(ns beetleman.rehydrate-test
  (:require [beetleman.rehydrate :as sut]
            [beetleman.rehydrate.async.core-async]
            [beetleman.rehydrate.async.manifold]
            [promesa.core :as p]
            [clojure.core.async :as a]
            [manifold.deferred :as d]
            [clojure.test :as t]))


(def result-factories
  [{:name "sync"
    :fn   identity}
   {:name "clojure.core/future"
    :fn   #(future %)}
   {:name "java.util.concurrent.CompletableFuture"
    :fn   #(p/future %)}
   {:name "clojure.core.async/chan"
    :fn   (fn [x]
            (let [ch (a/chan)]
              (a/put! ch x)
              ch))}
   {:name "manifold.deferred.Deferred"
    :fn   #(d/future %)}])


(def data
  [{:key 1
    :object {:object/to-hydrate-one-by-one 1}}
   {:key 2
    :object {:object/to-hydrate-one-by-one 2}}
   {:key 3
    :object {:object/to-hydrate-one-by-one 3}}])


(def hydrated-data
  [{:key 1
    :object {:object/to-hydrate-one-by-one {:hydrated/one 1}}}
   {:key 2
    :object {:object/to-hydrate-one-by-one {:hydrated/one 2}}}
   {:key 3
    :object {:object/to-hydrate-one-by-one {:hydrated/one 3}}}])


(defmethod sut/one :object/to-hydrate-one-by-one
  [_ {::keys [result-factory] :as _ctx} data]
  (result-factory {:hydrated/one data}))


(t/deftest find-all-test
  (t/is (= [{:target   [:object :object/to-hydrate-one-by-one]
             :location [0 :object :object/to-hydrate-one-by-one]
             :type     :object/to-hydrate-one-by-one
             :value    1}
            {:target   [:object :object/to-hydrate-one-by-one]
             :location [1 :object :object/to-hydrate-one-by-one]
             :type     :object/to-hydrate-one-by-one
             :value    2}
            {:target   [:object :object/to-hydrate-one-by-one]
             :location [2 :object :object/to-hydrate-one-by-one]
             :type     :object/to-hydrate-one-by-one
             :value    3}]
           (#'sut/find-all data [[:object :object/to-hydrate-one-by-one]]))))


(t/deftest replace-all-test
  (t/is (= hydrated-data
           (#'sut/replace-all
            data
            (#'sut/find-all data [[:object :object/to-hydrate-one-by-one]])
            {[:object/to-hydrate-one-by-one 1] {:hydrated/one 1}
             [:object/to-hydrate-one-by-one 2] {:hydrated/one 2}
             [:object/to-hydrate-one-by-one 3] {:hydrated/one 3}}))))


(t/deftest rehydrate-test
  (doseq [{:keys [fn name]} result-factories]
    (t/testing name
      (t/is (= {[:object/to-hydrate-one-by-one 1] {:hydrated/one 1}
                [:object/to-hydrate-one-by-one 2] {:hydrated/one 2}
                [:object/to-hydrate-one-by-one 3] {:hydrated/one 3}}
               @(#'sut/rehydrate
                 {::result-factory fn}
                 (#'sut/find-all data [[:object :object/to-hydrate-one-by-one]])))))))


(t/deftest run-test
  (doseq [{:keys [fn name]} result-factories]
    (t/testing name
               (t/is (= hydrated-data
                        @(sut/run {::result-factory fn}
                                  data
                                  [[:object :object/to-hydrate-one-by-one]]))))))
