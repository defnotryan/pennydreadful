(ns pennydreadful.test.data.collection
  (:require [expectations :refer :all]
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
