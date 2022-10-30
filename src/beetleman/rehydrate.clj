(ns beetleman.rehydrate
  (:require [com.rpl.specter :as sr]
            [promesa.core :as p]))


(defmulti many (fn [type _ctx _data] type))
(defmulti one (fn [type _ctx _data] type))


(defn- many-provided?
  [type]
  (contains? (methods many) type))


(defn- one-provided?
  [type]
  (contains? (methods one) type))


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
     (let [hydrated-value (get hydrated [type value] ::not-found)]
       (if-not (= hydrated-value ::not-found)
         (assoc-in data location hydrated-value)
         (throw (ex-info "Cant find data"
                         {:path     path
                          :hydrated hydrated
                          :type     ::hydrated-not-found})))))
   data
   paths))



(defn- rehydrate-many
  [ctx type paths]
  (p/then (many type ctx (mapv :value paths))
          (fn [data]
            (mapv (fn [{:keys [value]} x]
                    [[type value] x])
                  paths
                  data))))


(defn- rehydrate-one-by-one
  [ctx paths]
  (p/all (mapv
          (fn [{:keys [value type]}]
            (p/then (one type ctx value)
                    (fn [rehydrated]
                      [[type value] rehydrated])))
          paths)))


(defn- rehydrate
  [ctx paths]
  (-> (into []
            (map (fn [[type paths]]
                   (cond
                     (many-provided? type) (rehydrate-many ctx type paths)
                     (one-provided? type)  (rehydrate-one-by-one ctx paths))))
            (group-by :type
                      (into #{}
                            (map #(select-keys % [:type :value]))
                            paths)))
      p/all
      (p/then #(into {}
                     (mapcat identity)
                     %))))


(defn run
  [ctx data targets]
  (p/let [paths    (find-all data targets)
          hydrated (rehydrate ctx paths)]
    (replace-all data
                 paths
                 hydrated)))
