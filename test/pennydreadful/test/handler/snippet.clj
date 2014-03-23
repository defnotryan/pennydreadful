(ns pennydreadful.test.handler.snippet
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
            [pennydreadful.data.snippet :as data-snippet]
            [pennydreadful.test.handler.login :as login]))

(defn- make-post [body-content uri]
  (body (request :post uri) body-content))

(defn- post-new-to-collection [snippet collection-eid]
  (-> snippet
      (make-post (str "/collection/" collection-eid "/snippet"))
      app))

(defn- post-new-to-folder [snippet folder-eid]
  (-> snippet
      (make-post (str "/folder/" folder-eid "/snippet"))
      app))

;; POST snippet to collection returns 201 Created
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
     (post-new-to-collection {:name "SCENE 1"} coll-eid)))))

;; POST snippet to collection returns location of new snippet
(expect
 #"/snippet/[0-9]+"
 (login/as-ryan
   (let [{ryan-eid :id} (data-user/user-for-username "ryan")
         {coll-eid :id} (-> (data-project/projects-for-user-eid ryan-eid)
                            (find-where :name "accidental astronauts")
                            :id
                            (data-project/project-by-eid {:depth :collection})
                            :collections
                            (find-where :name "accidental astronauts manuscript"))]
     (-> {:name "SCENE 1"}
         (post-new-to-collection coll-eid)
         (get-in [:headers "Location"])))))

;; POST snippet to collection actually adds snippet to database
(expect
 #{"SCENE 1" "aa folder A" "aa snippet 1"}
 (login/as-ryan
  (let [{ryan-eid :id} (data-user/user-for-username "ryan")
        {coll-eid :id} (-> (data-project/projects-for-user-eid ryan-eid)
                           (find-where :name "accidental astronauts")
                           :id
                           (data-project/project-by-eid {:depth :collection})
                           :collections
                           (find-where :name "accidental astronauts manuscript"))]
    (post-new-to-collection {:name "SCENE 1"} coll-eid)
    (let [{children :children} (data-coll/collection-by-eid coll-eid {:depth :snippet-meta})]
      (into #{} (map :name children))))))

;; Cannot POST snippet to someone else's collection
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
     (post-new-to-collection {:name "SCENE 1"} coll-eid)))))


;; PUT snippet response
(expect
 {:status 201}
 (in
  (login/as-ryan
   (let [{ryan-eid :id} (data-user/user-for-username "ryan")
         {snippet-eid :id} (-> (data-project/projects-for-user-eid ryan-eid)
                               (find-where :name "accidental astronauts")
                               :id
                               (data-project/project-by-eid {:depth :snippet-meta})
                               :collections
                               (find-where :name "accidental astronauts manuscript")
                               :children
                               (find-where :name "aa snippet 1"))]
     (-> (request :put (str "/snippet/" snippet-eid))
         (body {:description "new description"})
         app)))))

;; PUT snippet changes database
(expect
 {:name "aa snippet 1" :description "new description"}
 (in
  (login/as-ryan
   (let [{ryan-eid :id} (data-user/user-for-username "ryan")
         {snippet-eid :id} (-> (data-project/projects-for-user-eid ryan-eid)
                               (find-where :name "accidental astronauts")
                               :id
                               (data-project/project-by-eid {:depth :snippet-meta})
                               :collections
                               (find-where :name "accidental astronauts manuscript")
                               :children
                               (find-where :name "aa snippet 1"))]
     (-> (request :put (str "/snippet/" snippet-eid))
         (body {:description "new description"})
         app)
     (data-snippet/snippet-by-eid snippet-eid)))))

;; Cannot PUT to someone else's snippet
(expect
 {:status 403}
 (in
  (login/as-ryan
   (let [{rhea-eid :id} (data-user/user-for-username "rhea")
         {snippet-eid :id} (-> (data-project/projects-for-user-eid rhea-eid)
                               (find-where :name "condescending cougars")
                               :id
                               (data-project/project-by-eid {:depth :snippet-meta})
                               :collections
                               (find-where :name "condescending cougars manuscript")
                               :children
                               (find-where :name "cc snippet 1"))]
     (-> (request :put (str "/snippet/" snippet-eid))
         (body {:name "malicious"})
         app)))))

(expect
 {:name "cc snippet 1" :description "condescending cougars snippet 1"}
 (in
  (login/as-ryan
   (let [{rhea-eid :id} (data-user/user-for-username "rhea")
         {snippet-eid :id} (-> (data-project/projects-for-user-eid rhea-eid)
                               (find-where :name "condescending cougars")
                               :id
                               (data-project/project-by-eid {:depth :snippet-meta})
                               :collections
                               (find-where :name "condescending cougars manuscript")
                               :children
                               (find-where :name "cc snippet 1"))]
     (-> (request :put (str "/snippet/" snippet-eid))
         (body {:name "malicious"})
         app)
     (data-snippet/snippet-by-eid snippet-eid)))))

;; PUT ignores :id in collection map, instead uses id in url
(expect
 {:name "aa snippet 1" :description "new description"}
 (in
  (login/as-ryan
   (let [{ryan-eid :id} (data-user/user-for-username "ryan")
         {snippet-eid :id} (-> (data-project/projects-for-user-eid ryan-eid)
                               (find-where :name "accidental astronauts")
                               :id
                               (data-project/project-by-eid {:depth :snippet-meta})
                               :collections
                               (find-where :name "accidental astronauts manuscript")
                               :children
                               (find-where :name "aa snippet 1"))]
     (-> (request :put (str "/snippet/" snippet-eid))
         (body {:id 0 :description "new description"})
         app)
     (data-snippet/snippet-by-eid snippet-eid)))))

