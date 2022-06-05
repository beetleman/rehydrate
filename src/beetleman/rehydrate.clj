(ns beetleman.rehydrate
  (:require [com.rpl.specter :as sr]))


(defmulti many (fn [type _ctx _data] type))
(defmulti one (fn [type _ctx _data] type))


(defrecord Location [index])


(def ^:private LocationSelector
  (sr/recursive-path [] p
    (sr/cond-path
      map?    [sr/ALL (sr/collect-one sr/FIRST) sr/LAST p]
      vector? [(sr/view #(map-indexed (fn [x y]
                                        [(->Location x) y])
                                      %))
               sr/ALL
               (sr/collect-one sr/FIRST)
               sr/LAST
               p]
      sr/STAY sr/STAY)))


(defn- find-all
  [data targets]
  (let [targets (set targets)]
    (into
     []
     (comp (map (fn [selected]
                  (let [location_    (butlast selected)
                        location     (map (fn [x]
                                            (if (instance? Location x)
                                              (:index x)
                                              x))
                                          location_)
                        [type value] (take-last 2 selected)
                        target       (remove #(instance? Location %) location_)]
                    {:location location
                     :target   target
                     :type     type
                     :value    value})))
           (filter (fn [{:keys [target]}]
                     (contains? targets target))))
     (sr/select LocationSelector data))))


(defn- replace-all
  [data paths hydrated]
  (reduce
   (fn [data {:keys [location type value] :as path}]
     (let [hydrated-value (get-in hydrated [type value] ::not-found)]
       (if-not (= hydrated-value ::not-found)
         (assoc-in data location hydrated-value)
         (throw (ex-info "Cant find data"
                         {:path path
                          :type ::hydrated-not-found})))))
   data
   paths))


(defn run
  [data]
  data)



(specter/select [specter/ALL :a :b]
                {:a {:b 3}}
)
