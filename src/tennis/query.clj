(ns tennis.query
  (:require [clojure.java.jdbc :as jdbc]))

(defn all-tennis-matches [db]
  (jdbc/query db ["SELECT * FROM tennis_match
                   ORDER BY 
                   tournament_year,
                   tournament_order, 
                   round_order desc,
                   match_order"]))

(defn tennis-macthes-by-winner-n-loser [db winner-id loser-id])

(defn select-max-elo [db]
  (jdbc/query db ["SELECT p.full_name, e.rating
                   FROM player p, elo e
                   where p.id = e.player_id"]
              {:result-set-fn (fn [rs]
                                (reduce (fn [{:keys [max-rating] :as acc}
                                             {:keys [full_name rating]}]
                                          (cond-> acc
                                            (< max-rating rating)
                                            (assoc :max-rating rating
                                                   :player-name full_name)))
                                        {:max-rating Integer/MIN_VALUE
                                         :player-name nil}
                                        rs))}))

(defn all-players [db]
  (jdbc/query db ["SELECT * FROM player"]))

(defn player [db id]
  (jdbc/query db ["SELECT * FROM player WHERE id = ?" id]))

(defn all-matches [db]
  (jdbc/query db ["SELECT * FROM tennis_match"]))

(defn match [db id]
  (jdbc/query db ["SELECT * FROM tennis_match WHERE id = ?" id]))

(defn matches-by-player [db player-id]
  (jdbc/query db ["SELECT * FROM tennis_match
                   WHERE winner_id = ? OR loser_id = ?" player-id player-id]))

(defn elo-by-player [db id]
  (jdbc/query db ["SELECT e.rating, e.id
                   FROM elo e, player p 
                   WHERE e.player_id = p.id AND
                   p.id = ? AND
                   e.id IN (SELECT MAX(e2.id) FROM elo e2 WHERE e2.player_id = ?)" 
                  id id]))