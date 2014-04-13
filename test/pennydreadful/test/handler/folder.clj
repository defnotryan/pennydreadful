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
            [pennydreadful.data.folder :as data-folder]
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

;; PUT folder response
(expect
 {:status 201}
 (in
  (login/as-ryan
   (let [{ryan-eid :id} (data-user/user-for-username "ryan")
         {folder-eid :id} (-> (data-project/projects-for-user-eid ryan-eid)
                              (find-where :name "accidental astronauts")
                              :id
                              (data-project/project-by-eid {:depth :snippet-meta})
                              :collections
                              (find-where :name "accidental astronauts manuscript")
                              :children
                              (find-where :name "aa folder A"))]
     (-> (request :put (str "/folder/" folder-eid))
         (body {:description "new description"})
         app)))))

;; PUT folder changes database
(expect
 {:name "aa folder A" :description "new description"}
 (in
  (login/as-ryan
   (let [{ryan-eid :id} (data-user/user-for-username "ryan")
         {folder-eid :id} (-> (data-project/projects-for-user-eid ryan-eid)
                              (find-where :name "accidental astronauts")
                              :id
                              (data-project/project-by-eid {:depth :snippet-meta})
                              :collections
                              (find-where :name "accidental astronauts manuscript")
                              :children
                              (find-where :name "aa folder A"))]
     (-> (request :put (str "/folder/" folder-eid))
         (body {:description "new description"})
         app)
     (data-folder/folder-by-eid folder-eid)))))

;; Cannot PUT to someone else's folder
(expect
 {:status 403}
 (in
  (login/as-ryan
   (let [{rhea-eid :id} (data-user/user-for-username "rhea")
         {folder-eid :id} (-> (data-project/projects-for-user-eid rhea-eid)
                              (find-where :name "condescending cougars")
                              :id
                              (data-project/project-by-eid {:depth :snippet-meta})
                              :collections
                              (find-where :name "condescending cougars manuscript")
                              :children
                              (find-where :name "cc folder A"))]
     (-> (request :put (str "/folder/" folder-eid))
         (body {:name "malicious"})
         app)))))

(expect
 {:name "cc folder A" :description "condescending cougars folder A"}
 (in
  (login/as-ryan
   (let [{rhea-eid :id} (data-user/user-for-username "rhea")
         {folder-eid :id} (-> (data-project/projects-for-user-eid rhea-eid)
                              (find-where :name "condescending cougars")
                              :id
                              (data-project/project-by-eid {:depth :snippet-meta})
                              :collections
                              (find-where :name "condescending cougars manuscript")
                              :children
                              (find-where :name "cc folder A"))]
     (-> (request :put (str "/folder/" folder-eid))
         (body {:name "malicious"})
         app)
     (data-folder/folder-by-eid folder-eid)))))

;; PUT ignores :id in collection map, instead uses id in url
(expect
 {:name "aa folder A" :description "new description"}
 (in
  (login/as-ryan
   (let [{ryan-eid :id} (data-user/user-for-username "ryan")
         {folder-eid :id} (-> (data-project/projects-for-user-eid ryan-eid)
                              (find-where :name "accidental astronauts")
                              :id
                              (data-project/project-by-eid {:depth :snippet-meta})
                              :collections
                              (find-where :name "accidental astronauts manuscript")
                              :children
                              (find-where :name "aa folder A"))]
     (-> (request :put (str "/folder/" folder-eid))
         (body {:id 0 :description "new description"})
         app)
     (data-folder/folder-by-eid folder-eid)))))

;; PUT /folder/:folder-eid/move-up response
(expect
 {:status 201}
 (in
  (login/as-ryan
   (let [{ryan-eid :id} (data-user/user-for-username "ryan")
         {folder-eid :id} (-> (data-project/projects-for-user-eid ryan-eid)
                              (find-where :name "accidental astronauts")
                              :id
                              (data-project/project-by-eid {:depth :snippet-meta})
                              :collections
                              (find-where :name "accidental astronauts research")
                              :children
                              (find-where :name "aa folder B")
                              :children
                              (find-where :name "aa folder BB"))]
     (-> (request :put (str "/folder/" folder-eid "/move-up"))
         app)))))

;; PUT /folder/:folder-eid/move-down response
(expect
 {:status 201}
 (in
  (login/as-ryan
   (let [{ryan-eid :id} (data-user/user-for-username "ryan")
         {folder-eid :id} (-> (data-project/projects-for-user-eid ryan-eid)
                              (find-where :name "accidental astronauts")
                              :id
                              (data-project/project-by-eid {:depth :snippet-meta})
                              :collections
                              (find-where :name "accidental astronauts research")
                              :children
                              (find-where :name "aa folder B")
                              :children
                              (find-where :name "aa folder BA"))]
     (-> (request :put (str "/folder/" folder-eid "/move-down"))
         app)))))

;; Can't move-up someone else's folder
(expect
 {:status 401}
 (in
  (login/as-ryan
   (let [{rhea-eid :id} (data-user/user-for-username "rhea")
         {folder-eid :id} (-> (data-project/projects-for-user-eid rhea-eid)
                              (find-where :name "condescending cougars")
                              :id
                              (data-project/project-by-eid {:depth :snippet-meta})
                              :collections
                              (find-where :name "condescending cougars research")
                              :children
                              (find-where :name "cc folder B")
                              :children
                              (find-where :name "cc folder BB"))]
     (-> (request :put (str "/folder/" folder-eid "/move-up"))
         app)))))

;; Can't move-down someone else's folder
(expect
 {:status 401}
 (in
  (login/as-ryan
   (let [{rhea-eid :id} (data-user/user-for-username "rhea")
         {folder-eid :id} (-> (data-project/projects-for-user-eid rhea-eid)
                              (find-where :name "condescending cougars")
                              :id
                              (data-project/project-by-eid {:depth :snippet-meta})
                              :collections
                              (find-where :name "condescending cougars research")
                              :children
                              (find-where :name "cc folder B")
                              :children
                              (find-where :name "cc folder BA"))]
     (-> (request :put (str "/folder/" folder-eid "/move-down"))
         app)))))

;; PUT to /folder/:folder-eid/move-up mutates database correctly
(expect
 {:name "aa folder BB"}
 (in
  (login/as-ryan
   (let [{ryan-eid :id} (data-user/user-for-username "ryan")
         {folder-eid :id} (-> (data-project/projects-for-user-eid ryan-eid)
                              (find-where :name "accidental astronauts")
                              :id
                              (data-project/project-by-eid {:depth :snippet-meta})
                              :collections
                              (find-where :name "accidental astronauts research")
                              :children
                              (find-where :name "aa folder B")
                              :children
                              (find-where :name "aa folder BB"))]
     (-> (request :put (str "/folder/" folder-eid "/move-up"))
         app)
     (-> (data-project/projects-for-user-eid ryan-eid)
         (find-where :name "accidental astronauts")
         :id
         (data-project/project-by-eid {:depth :snippet-meta})
         :collections
         (find-where :name "accidental astronauts research")
         :children
         (find-where :name "aa folder B")
         :children
         (find-where :position 0))))))

;; PUT to /folder/:folder-eid/move-down mutates database correctly
(expect
 {:name "aa folder BB"}
 (in
  (login/as-ryan
   (let [{ryan-eid :id} (data-user/user-for-username "ryan")
         {folder-eid :id} (-> (data-project/projects-for-user-eid ryan-eid)
                              (find-where :name "accidental astronauts")
                              :id
                              (data-project/project-by-eid {:depth :snippet-meta})
                              :collections
                              (find-where :name "accidental astronauts research")
                              :children
                              (find-where :name "aa folder B")
                              :children
                              (find-where :name "aa folder BA"))]
     (-> (request :put (str "/folder/" folder-eid "/move-down"))
         app)
     (-> (data-project/projects-for-user-eid ryan-eid)
         (find-where :name "accidental astronauts")
         :id
         (data-project/project-by-eid {:depth :snippet-meta})
         :collections
         (find-where :name "accidental astronauts research")
         :children
         (find-where :name "aa folder B")
         :children
         (find-where :position 0))))))
