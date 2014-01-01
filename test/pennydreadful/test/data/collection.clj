(ns pennydreadful.test.data.collection
  (:require [expectations :refer :all]
            [pennydreadful.util :refer [mapped? pprint-str]]
            [pennydreadful.test.util :refer :all]
            [pennydreadful.data.user :as data-user]
            [pennydreadful.data.project :as data-project]
            [pennydreadful.data.collection :refer :all]))

;; Insert collection
(expect
 {:name "manuscript" :description "cheddarsled manuscript"}
 (in
  (with-populated-db
    (let [{ryan-eid :id} (data-user/user-for-username "ryan")
          {cheddar-eid :id} (data-project/insert-project! ryan-eid {:name "cheddarsled" :description "a christmas tradition"})
          {manuscript-eid :id} (insert-collection! cheddar-eid {:name "manuscript" :description "cheddarsled manuscript"})]
      (collection-by-eid manuscript-eid)))))


 ;; Retrieve via project
 (expect
  {:name "manuscript" :description "cheddarsled manuscript"}
  (in
   (with-populated-db
     (let [{ryan-eid :id} (data-user/user-for-username "ryan")
           {cheddar-eid :id} (data-project/insert-project! ryan-eid {:name "cheddarsled"})]
       (insert-collection! cheddar-eid {:name "manuscript" :description "cheddarsled manuscript"})
       (-> cheddar-eid
           (data-project/project-by-eid {:depth :collection})
           :collections
           first)))))

 (expect
  #{"manuscript" "research"}
  (with-populated-db
    (let [{ryan-eid :id} (data-user/user-for-username "ryan")
          {cheddar-eid :id} (data-project/insert-project! ryan-eid {:name "cheddarsled"})]
      (insert-collection! cheddar-eid {:name "manuscript" :description "cheddarsled manuscript"})
      (insert-collection! cheddar-eid {:name "research" :description "cheddarsled research"})
      (let [collections (-> cheddar-eid
                            (data-project/project-by-eid {:depth :collection})
                            :collections)]
        (into #{} (map :name collections))))))

 (defn- find-where [ms k v]
   "Given a seq of maps ms, returns the first map where key k is mapped to value v."
   (some #(mapped? % k v) ms))

 ;; Retrieve nested
 (expect
  "aa snippet A1"
  (with-populated-db
    (let [{ryan-eid :id} (data-user/user-for-username "ryan")]
      (-> (data-project/projects-for-user-eid ryan-eid)
          (find-where :name "accidental astronauts")
          :id
          (data-project/project-by-eid {:depth :snippet-names})
          :collections
          (find-where :name "accidental astronauts manuscript")
          :children
          (find-where :name "aa folder A")
          :children
          (find-where :name "aa snippet A1")
          :name))))

 (expect
  "aa snippet BA2"
  (with-populated-db
    (let [{ryan-eid :id} (data-user/user-for-username "ryan")]
      (-> (data-project/projects-for-user-eid ryan-eid)
          (find-where :name "accidental astronauts")
          :id
          (data-project/project-by-eid {:depth :snippet-names})
          :collections
          (find-where :name "accidental astronauts research")
          :children
          (find-where :name "aa folder B")
          :children
          (find-where :name "aa folder BA")
          :children
          (find-where :name "aa snippet BA2")
          :name))))

 #_(expect
  "impossible"
  (with-populated-db
    (let [{ryan-eid :id} (data-user/user-for-username "ryan")]
      (-> (data-project/projects-for-user-eid ryan-eid)
          (find-where :name "accidental astronauts")
          :id
          (data-project/project-by-eid {:depth :snippet-names})
          pprint-str
          print))))
