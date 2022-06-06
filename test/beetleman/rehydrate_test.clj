(ns beetleman.rehydrate-test
  (:require [beetleman.rehydrate :as sut]
            [promesa.core :as p]
            [clojure.test :as t]))


(def data
  [{:foo 1
    :bar {:bar/baz 1}}])


(def bar-baz ::bar-baz-hydrated)


(def hydrated-data
  [{:foo 1
    :bar {:bar/baz bar-baz}}])


(defmethod sut/one :bar/baz
  [_ _ctx _data]
  (p/promise bar-baz))


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
  (t/is (= {[:bar/baz 1] bar-baz}
           @(#'sut/rehydrate {} (#'sut/find-all data [[:bar :bar/baz]])))))
