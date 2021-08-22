(ns tennis.api
  (:require [clojure.edn :as edn]
            [compojure.core :refer [context defroutes DELETE GET PUT POST]]
            [compojure.route :as route]
            [muuntaja.middleware :as middleware]
            [tennis.ingest :as ingest]
            [tennis.query :as query]
            [tennis.database :as database]
            [tennis.elo :as elo]
            [ring.adapter.jetty :refer [run-jetty]]
            [ring.middleware.params :as params]
            [jumblerg.middleware.cors :refer [wrap-cors]]))

(defroutes routes
  (context "/players" []
    (GET "/" []
      {:body (query/all-players database/db)})
    (GET "/:id" [id]
      (when-first [user (query/player database/db id)]
        {:body user}))
    (GET "/:id/matches" [id]
      {:body (query/matches-by-player database/db id)})
    (GET "/:id/elo" [id]
      (when-first [elo (query/elo-by-player database/db id)]
        {:body elo})))
  (context "/tennis-matches" []
    (GET "/" []
      {:body (query/all-matches database/db)})
    (GET "/:id" [id]
      (when-first [match (query/match database/db id)]
        {:body match}))
    (PUT "/:id" req
      (let [id (-> req :params :id)
            {:keys [winner_id loser_id] :as tennis-match} (-> (slurp (:body req))
                                                              edn/read-string
                                                              (assoc :id id))
            [{winner-elo :rating}] (query/elo-by-player database/db winner_id)
            [{loser-elo :rating}] (query/elo-by-player database/db loser_id)
            new-player-ratings (elo/calculate-new-ratings {winner_id winner-elo
                                                           loser_id loser-elo}
                                                          tennis-match)]
        (ingest/tennis-match database/db tennis-match)
        (elo/persist database/db new-player-ratings)
        {:status 201
         :headers {"Link" (str "/tennis-matches/" id)}})))
  (route/not-found "Not found"))

(defn run
  ([] (run (or (System/getenv "PORT") "7000")))
  ([port]
   (println "Starting server at port:" port)
   (run-jetty (-> routes
                  middleware/wrap-format
                  params/wrap-params
                  (wrap-cors ".*")
                  (wrap-cors identity))
              {:port (new Integer port)
               :join? false})))