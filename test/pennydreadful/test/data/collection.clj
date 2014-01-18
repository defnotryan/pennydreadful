(ns pennydreadful.test.data.collection
  (:require [expectations :refer :all]
            [pennydreadful.util :refer [pprint-str]]
            [pennydreadful.test.util :refer :all]
            [pennydreadful.data.user :as data-user]
            [pennydreadful.data.project :as data-project]
            [pennydreadful.data.collection :refer :all]))

;; Insert collection
(expect
 {:name "manuscript" :description "cheddarsled manuscript" :position 0}
 (in
  (with-populated-db
    (let [{ryan-eid :id} (data-user/user-for-username "ryan")
          {cheddar-eid :id} (data-project/insert-project! ryan-eid {:name "cheddarsled" :description "a christmas tradition"})
          {manuscript-eid :id} (insert-collection! cheddar-eid {:name "manuscript" :description "cheddarsled manuscript"})]
      (collection-by-eid manuscript-eid)))))

;; Insert collection appends correctly
(expect
 {:name "research" :description "cheddarsled research" :position 1}
 (in
  (with-populated-db
    (let [{ryan-eid :id} (data-user/user-for-username "ryan")
          {cheddar-eid :id} (data-project/insert-project! ryan-eid {:name "cheddarsled" :description "a christmas tradition"})
          {manuscript-eid :id} (insert-collection! cheddar-eid {:name "manuscript" :description "cheddarsled manuscript"})
          {research-eid :id} (insert-collection! cheddar-eid {:name "research" :description "cheddarsled research"})]
      (collection-by-eid research-eid)))))

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

;; Ownership
(expect
 true
 (with-populated-db
   (let [{ryan-eid :id} (data-user/user-for-username "ryan")
         {coll-eid :id} (-> (data-project/projects-for-user-eid ryan-eid)
                            (find-where :name "accidental astronauts")
                            :id
                            (data-project/project-by-eid {:depth :collection})
                            :collections
                            (find-where :name "accidental astronauts manuscript"))]
     (collection-eid-owned-by-user-eid? coll-eid ryan-eid))))

(expect
 false
 (with-populated-db
   (let [{ryan-eid :id} (data-user/user-for-username "ryan")
         {rhea-eid :id} (data-user/user-for-username "rhea")
         {coll-eid :id} (-> (data-project/projects-for-user-eid ryan-eid)
                            (find-where :name "accidental astronauts")
                            :id
                            (data-project/project-by-eid {:depth :collection})
                            :collections
                            (find-where :name "accidental astronauts manuscript"))]
     (collection-eid-owned-by-user-eid? coll-eid rhea-eid))))

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

 ;; Use this to pretty print an entire project hierarchy.
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

;; Delete collection
(expect
 {}
 (with-populated-db
   (let [{ryan-eid :id} (data-user/user-for-username "ryan")
         {coll-eid :id} (-> (data-project/projects-for-user-eid ryan-eid)
                            (find-where :name "accidental astronauts")
                            :id
                            (data-project/project-by-eid {:depth :collection})
                            :collections
                            (find-where :name "accidental astronauts research"))]
     (delete-collection! coll-eid)
     (select-keys (collection-by-eid coll-eid) [:name :description]))))

;; Delete collection elides position correctly
(expect
 {:name "accidental astronauts research" :position 0}
 (in
  (with-populated-db
    (let [{ryan-eid :id} (data-user/user-for-username "ryan")
          collections (-> (data-project/projects-for-user-eid ryan-eid)
                          (find-where :name "accidental astronauts")
                          :id
                          (data-project/project-by-eid {:depth :collection})
                          :collections)
          {manuscript-eid :id} (find-where collections :name "accidental astronauts manuscript")
          {research-eid :id} (find-where collections :name "accidental astronauts research")]
      (delete-collection! manuscript-eid)
      (collection-by-eid research-eid)))))

;; Update a collection
(expect
 {:name "accidental astronauts manuscript" :description "the new description"}
 (in
  (with-populated-db
    (let [{ryan-eid :id} (data-user/user-for-username "ryan")
          collection (-> (data-project/projects-for-user-eid ryan-eid)
                         (find-where :name "accidental astronauts")
                         :id
                         (data-project/project-by-eid {:depth :collection})
                         :collections
                         (find-where :name "accidental astronauts manuscript"))]
      (update-collection! (assoc collection :description "the new description"))
      (collection-by-eid (:id collection))))))

(expect
 {:name "accidental astronauts new manuscript" :description "description here"}
 (in
  (with-populated-db
    (let [{ryan-eid :id} (data-user/user-for-username "ryan")
          collection (-> (data-project/projects-for-user-eid ryan-eid)
                         (find-where :name "accidental astronauts")
                         :id
                         (data-project/project-by-eid {:depth :collection})
                         :collections
                         (find-where :name "accidental astronauts manuscript"))]
      (update-collection! (assoc collection :name "accidental astronauts new manuscript"))
      (collection-by-eid (:id collection))))))

(expect
 {:name "accidental astronauts new manuscript" :description "the new description"}
 (in
  (with-populated-db
    (let [{ryan-eid :id} (data-user/user-for-username "ryan")
          collection (-> (data-project/projects-for-user-eid ryan-eid)
                         (find-where :name "accidental astronauts")
                         :id
                         (data-project/project-by-eid {:depth :collection})
                         :collections
                         (find-where :name "accidental astronauts manuscript"))]
      (update-collection!
       (assoc collection
         :name "accidental astronauts new manuscript"
         :description "the new description"))
      (collection-by-eid (:id collection))))))

;; Move collection up

(expect
 ["accidental astronauts research" "accidental astronauts manuscript"]
 (with-populated-db
   (let [{ryan-eid :id} (data-user/user-for-username "ryan")
         project (-> (data-project/projects-for-user-eid ryan-eid)
                     (find-where :name "accidental astronauts")
                     :id
                     (data-project/project-by-eid {:depth :collection}))
         research (find-where (:collections project) :name "accidental astronauts research")]
     (move-up! (:id research))
     (let [collections (-> (data-project/projects-for-user-eid ryan-eid)
                           (find-where :name "accidental astronauts")
                           :id
                           (data-project/project-by-eid {:depth :collection})
                           :collections)]
       (->> collections
            (sort-by :position)
            (map :name))))))

;; Move collection down
(expect
 ["accidental astronauts research" "accidental astronauts manuscript"]
 (with-populated-db
   (let [{ryan-eid :id} (data-user/user-for-username "ryan")
         project (-> (data-project/projects-for-user-eid ryan-eid)
                     (find-where :name "accidental astronauts")
                     :id
                     (data-project/project-by-eid {:depth :collection}))
         manuscript (find-where (:collections project) :name "accidental astronauts manuscript")]
     (move-down! (:id manuscript))
     (let [collections (-> (data-project/projects-for-user-eid ryan-eid)
                           (find-where :name "accidental astronauts")
                           :id
                           (data-project/project-by-eid {:depth :collection})
                           :collections)]
       (->> collections
            (sort-by :position)
            (map :name))))))