(ns pennydreadful.test.handler.folder
  (:require [expectations :refer :all]
            [ring.mock.request :refer :all]
            [clojure.edn :as edn]
            [clj-time.coerce :as tc]
            [pennydreadful.handler :refer [app]]
            [pennydreadful.test.util :refer :all]
            [pennydreadful.data.user :as data-user]
            [pennydreadful.data.project :as data-project]
            [pennydreadful.data.collection :as data-coll]
            [pennydreadful.test.handler.login :as login]))

(defn- make-post [body-content uri]
  (body (request :post uri) body-content))

(defn- post-new-to-collection [folder collection-eid]
  (-> folder
      (make-post (str "/collection/" collection-eid "/folder"))
      app))

(defn- post-new-to-folder [folder folder-eid]
  (-> folder
      (make-post (str "/folder/" folder-eid "/folder"))
      app))

;; POST folder to collection returns 201 Created
(expect
 {:status 201}
 (in
  (login/as-ryan
   (let [{ryan-eid :id} (data-user/user-for-username "ryan")
         {coll-eid :id} (-> (data-project/projects-for-user-eid ryan-eid)
                            (find-where :name "accidental astronauts")
                            :id
                            (data-project/project-by-eid {:depth :collection})
                            :collections
                            (find-where :name "accidental astronauts manuscript"))]
     (post-new-to-collection {:name "CHAPTER 1"} coll-eid)))))

;; POST folder to collection returns location of new folder
(expect
 #"/folder/[0-9]+"
 (login/as-ryan
  (let [{ryan-eid :id} (data-user/user-for-username "ryan")
        {coll-eid :id} (-> (data-project/projects-for-user-eid ryan-eid)
                           (find-where :name "accidental astronauts")
                           :id
                           (data-project/project-by-eid {:depth :collection})
                           :collections
                           (find-where :name "accidental astronauts manuscript"))]
   (-> {:name "CHAPTER 1"}
       (post-new-to-collection coll-eid)
       (get-in [:headers "Location"])))))

;; POST folder to collection actually adds folder to database
(expect
 #{"CHAPTER 1" "aa folder A" "aa snippet 1"}
 (login/as-ryan
  (let [{ryan-eid :id} (data-user/user-for-username "ryan")
        {coll-eid :id} (-> (data-project/projects-for-user-eid ryan-eid)
                           (find-where :name "accidental astronauts")
                           :id
                           (data-project/project-by-eid {:depth :collection})
                           :collections
                           (find-where :name "accidental astronauts manuscript"))]
    (post-new-to-collection {:name "CHAPTER 1"} coll-eid)
    (let [{children :children} (data-coll/collection-by-eid coll-eid {:depth :snippet-meta})]
      (into #{} (map :name children))))))

;; Cannot POST folder to someone else's collection
(expect
 {:status 401}
 (in
  (login/as-ryan
   (let [{ryan-eid :id} (data-user/user-for-username "ryan")
         {rhea-eid :id} (data-user/user-for-username "rhea")
         {coll-eid :id} (-> (data-project/projects-for-user-eid rhea-eid)
                            (find-where :name "condescending cougars")
                            :id
                            (data-project/project-by-eid {:depth :collection})
                            :collections
                            (find-where :name "condescending cougars manuscript"))]
     (post-new-to-collection {:name "CHAPTER 1"} coll-eid)))))
