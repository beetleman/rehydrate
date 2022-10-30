(ns beetleman.rehydrate-test
  (:require [beetleman.rehydrate :as sut]
            [promesa.core :as p]
            [clojure.test :as t]))


(def result-factories
  [{:name "sync"
    :fn   identity}
   {:name "clojure.core/future"
    :fn   #(future %)}
   {:name "java.util.concurrent.CompletableFuture"
    :fn   #(p/future %)}])


(def data
  [{:foo 1
    :bar {:bar/baz 1}}])


(def bar-baz ::bar-baz-hydrated)


(def hydrated-data
  [{:foo 1
    :bar {:bar/baz bar-baz}}])


(defmethod sut/one :bar/baz
  [_ {::keys [result-factory] :as _ctx} _data]
  (result-factory bar-baz))


(t/deftest find-all-test
  (t/is (= [{:target   [:bar :bar/baz]
             :location [0 :bar :bar/baz]
             :type     :bar/baz
             :value    1}]
           (#'sut/find-all data [[:bar :bar/baz]]))))


(t/deftest replace-all-test
  (t/is (= hydrated-data
           (#'sut/replace-all
            data
            (#'sut/find-all data [[:bar :bar/baz]])
            {[:bar/baz 1] bar-baz}))))


(t/deftest rehydrate-test
  (doseq [{:keys [fn name]} result-factories]
    (t/testing name
               (t/is (= {[:bar/baz 1] bar-baz}
                        @(#'sut/rehydrate
                          {::result-factory fn}
                          (#'sut/find-all data [[:bar :bar/baz]])))))))


(t/deftest run-test
  (doseq [{:keys [fn name]} result-factories]
    (t/testing name
               (t/is (= hydrated-data
                        @(sut/run {::result-factory fn}
                                  data
                                  [[:bar :bar/baz]]))))))
