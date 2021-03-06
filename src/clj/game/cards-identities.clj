(in-ns 'game.core)

(def cards-identities
  {"Adam: Compulsive Hacker"
   (let [titles #{"Safety First" "Always Be Running" "Neutralize All Threats"}
         one-of-each   (fn [cards] (->> cards (group-by :title) (map second) (map first)))
         get-directives (fn [source] (filter #(some #{(:title %)} titles) source))]
   {:effect (req (let [directives (-> (:deck runner) (concat (:hand runner)) (get-directives) one-of-each)]
                   (doseq [c directives]
                     (runner-install state side c {:no-cost true
                                                   :custom-message (str "starts with " (:title c) " in play")}))
                   (draw state :runner (count (filter #(= (:zone %) [:hand]) directives)))))})

   "Andromeda: Dispossessed Ristie"
   {:effect (effect (gain :link 1) (draw 4)) :mulligan (effect (draw 4))}

   "Apex: Invasive Predator"
   {:events {:runner-turn-begins
              {:prompt "Select a card to install facedown"
               :choices {:max 1 :req #(and (:side % "Runner") (= (:zone %) [:hand]))}
               :req (req (> (count (:hand runner)) 0))
               :effect (req (runner-install state side target {:facedown true}))}}}

   "Argus Security: Protection Guaranteed"
   {:events {:agenda-stolen
             {:prompt "Take 1 tag or suffer 2 meat damage?"
              :choices ["1 tag" "2 meat damage"] :player :runner
              :msg "make the Runner take 1 tag or suffer 2 meat damage"
              :effect (req (if (= target "1 tag")
                             (do (tag-runner state :runner 1) (system-msg state side "takes 1 tag"))
                             (do (damage state :runner :meat 2 {:unboostable true :card card})
                                 (system-msg state side "suffers 2 meat damage"))))}}}

   "Armand \"Geist\" Walker: Tech Lord"
   {:effect (effect (gain :link 1))
    :events {:runner-trash {:req (req (and (= side :runner) (= (second targets) :ability-cost)))
                            :msg "draw a card"
                            :effect (effect (draw 1))}}}

   "Blue Sun: Powering the Future"
   {:abilities [{:choices {:req #(:rezzed %)}
                 :effect (req (trigger-event state side :pre-rez-cost target)
                              (let [cost (rez-cost state side target)]
                                (gain state side :credit cost)
                                (move state side target :hand)
                                (system-msg state side (str "add " (:title target) " to HQ and gain " cost " [Credits]"))
                                (swap! state update-in [:bonus] dissoc :cost)))}]}

   "Cerebral Imaging: Infinite Frontiers"
   {:effect (req (add-watch state :cerebral-imaging
                            (fn [k ref old new]
                              (let [credit (get-in new [:corp :credit])]
                                (when (not= (get-in old [:corp :credit]) credit)
                                  (swap! ref assoc-in [:corp :max-hand-size] credit))))))}

   "Chaos Theory: Wünderkind"
   {:effect (effect (gain :memory 1))}

   "Cybernetics Division: Humanity Upgraded"
   {:effect (effect (lose :max-hand-size 1) (lose :runner :max-hand-size 1))}

   "Edward Kim: Humanitys Hammer"
   {:effect (effect (gain :link 1))
    :events {:access {:once :per-turn
                      :req (req (= (:type target) "Operation"))
                      :effect (effect (trash target))
                      :msg (msg "trash " (:title target) (if (some #{:discard} (:zone target)) ", but it is already trashed."))}}}

   "Exile: Streethawk"
   {:effect (effect (gain :link 1))
    :events {:runner-install {:req (req (and (has? target :type "Program")
                                             (some #{:discard} (:previous-zone target))))
                              :msg (msg "draw a card") :effect (effect (draw 1))}}}

   "Gabriel Santiago: Consummate Professional"
   {:events {:successful-run {:msg "gain 2 [Credits]" :once :per-turn
                              :effect (effect (gain :credit 2)) :req (req (= target :hq))}}}

   "Gagarin Deep Space: Expanding the Horizon"
   {:events {:pre-access-card {:req (req (= (second (:zone target)) :remote))
                               :effect (effect (access-cost-bonus [:credit 1]))}}}

   "GRNDL: Power Unleashed"
   {:effect (effect (gain :credit 5 :bad-publicity 1))}

   "Haarpsichord Studios"
   {:events {:pre-steal-cost {:req (req (:stole-agenda runner-reg))
                              :effect (effect (prevent-steal))}}}

   "Haas-Bioroid: Engineering the Future"
   {:events {:corp-install {:once :per-turn :msg "gain 1 [Credits]"
                            :effect (effect (gain :credit 1))}}}

   "Haas-Bioroid: Stronger Together"
   {:events {:pre-ice-strength {:req (req (and (= (:type target) "ICE") (has? target :subtype "Bioroid")))
                                :effect (effect (ice-strength-bonus 1))}}}

   "Harmony Medtech: Biomedical Pioneer"
   {:effect (effect (lose :agenda-point-req 1) (lose :runner :agenda-point-req 1))}

   "Hayley Kaplan: Universal Scholar"
   {:events {:runner-install
             {:optional {:prompt (msg "Install another " (:type target) " from Grip?")
                         :req (req (and (first-event state side :runner-install) ;; If this is the first installation of the turn
                                        (some #(= (:type  %) (:type target)) (:hand runner)))) ;; and there are additional cards of that type in hand
                         :yes-ability {:effect (req (let [type (:type target)]
                                              (resolve-ability
                                               state side
                                               {:prompt (msg "Choose a " type " to install")
                                                :choices (req (filter #(has? % :type type) (:hand runner)))
                                                :msg (msg "install " (:title target))
                                                :effect (effect (runner-install target))} card nil)))}}}}}

   "Iain Stirling: Retired Spook"
   {:effect (effect (gain :link 1))
    :events {:runner-turn-begins {:req (req (> (:agenda-point corp) (:agenda-point runner)))
                                  :msg "to gain 2 [Credits]" :effect (effect (gain :credit 2))}}}

   "Industrial Genomics: Growing Solutions"
   {:events {:pre-trash {:effect (effect (trash-cost-bonus
                                           (count (filter #(not (:seen %)) (:discard corp)))))}}}

   "Jinteki: Personal Evolution"
   {:events {:agenda-scored {:msg "do 1 net damage" :effect (effect (damage :net 1 {:card card}))}
             :agenda-stolen {:msg "do 1 net damage" :effect (effect (damage :net 1 {:card card}))}}}

   "Jinteki Biotech: Life Imagined"
   {:events {:pre-first-turn {:req (req (= side :corp))
                              :prompt "Choose a copy of Jinteki Biotech to use this game"
                              :choices ["[The Brewery~brewery]" "[The Tank~tank]" "[The Greenhouse~greenhouse]"]
                              :effect (effect (update! (assoc card :biotech-target target))
                                              (system-msg (str "has chosen a copy of Jinteki Biotech for this game ")))}}
    :abilities [{:cost [:click 3]
                 :req (req (not (:biotech-used card)))
                 :effect (req (let [flip (:biotech-target card)]
                                (update! state side (assoc card :biotech-used true))
                                (case flip
                                  "[The Brewery~brewery]"
                                  (do (system-msg state side "uses [The Brewery~brewery] to do 2 net damage")
                                      (damage state side :net 2 {:card card})
                                      (update! state side (assoc card :code "brewery")))
                                  "[The Tank~tank]"
                                  (do (system-msg state side "uses [The Tank~tank] to shuffle Archives into R&D")
                                      (shuffle-into-deck state side :discard)
                                      (update! state side (assoc card :code "tank")))
                                  "[The Greenhouse~greenhouse]"
                                  (do (system-msg state side (str "uses [The Greenhouse~greenhouse] to place 4 advancement tokens "
                                                                  "on a card that can be advanced"))
                                      (update! state side (assoc card :code "greenhouse"))
                                      (resolve-ability
                                        state side
                                        {:prompt "Choose a card that can be advanced"
                                         :choices {:req #(or (= (:advanceable %) "always")
                                                             (and (= (:advanceable %) "while-rezzed") (:rezzed %))
                                                             (= (:type %) "Agenda"))}
                                         :effect (effect (add-prop target :advance-counter 4))} card nil)))))}]}

   "Kate \"Mac\" McCaffrey: Digital Tinker"
   {:effect (effect (gain :link 1))
    :events {:pre-install {:req (req (and (#{"Hardware" "Program"} (:type target))
                                          (not (get-in @state [:per-turn (:cid card)]))))
                           :effect (effect (install-cost-bonus [:credit -1]))}
             :runner-install {:req (req (and (#{"Hardware" "Program"} (:type target))
                                             (not (get-in @state [:per-turn (:cid card)]))))
                              :msg (msg "reduce the install cost of " (:title target) " by 1 [Credits]")
                              :effect (req (swap! state assoc-in [:per-turn (:cid card)] true))}}}

   "Ken \"Express\" Tenma: Disappeared Clone"
   {:events {:play-event {:req (req (has? target :subtype "Run")) :once :per-turn
                          :msg "gain 1 [Credits]" :effect (effect (gain :credit 1))}}}

   "Laramy Fisk: Savvy Investor"
   {:events {:no-action {:effect (effect (system-msg "can be forced to draw by clicking on Laramy Fisk"))
                         :req (req (and run
                                        (some #{:hq :rd :archives} (:server run))
                                        (not current-ice)
                                        (not (get-in @state [:per-turn (:cid card)]))
                                        (empty? (let [successes (map first (turn-events state side :successful-run))]
                                                  (filter #(not (= % :remote)) successes)))))}}
    :abilities [{:msg "force the Corp to draw 1 card"
                 :req (req (and run
                                (some #{:hq :rd :archives} (:server run))
                                (:no-action run)
                                (not current-ice)
                                (not (get-in @state [:per-turn (:cid card)]))
                                (empty? (let [successes (map first (turn-events state side :successful-run))]
                                          (filter #(not (= % :remote)) successes)))))
                 :effect (req (effect (draw :corp)) (swap! state assoc-in [:per-turn (:cid card)] true))}]}

   "Leela Patel: Trained Pragmatist"
   {:events {:agenda-scored {:choices {:req #(and (not (:rezzed %)) (= (:side %) "Corp"))} :msg "add 1 unrezzed card to HQ"
                             :player :runner :effect (effect (move :corp target :hand))}
             :agenda-stolen {:choices {:req #(and (not (:rezzed %)) (= (:side %) "Corp"))} :msg "add 1 unrezzed card to HQ"
                             :effect (req (move state :corp target :hand)
                                          (swap! state update-in [:runner :prompt] rest)
                                          (handle-end-run state side))}}}

   "MaxX: Maximum Punk Rock"
   {:events {:runner-turn-begins {:msg "trash the top 2 cards from Stack and draw 1 card"
                                  :effect (effect (mill 2) (draw))}}}

   "Nasir Meidan: Cyber Explorer"
   {:effect (effect (gain :link 1))
    :abilities [{:req (req (and (:run @state)
                                (:rezzed (get-card state current-ice))))
                 :effect (req (let [current-ice (get-card state current-ice)]
                           (trigger-event state side :pre-rez-cost current-ice)
                           (let [cost (rez-cost state side current-ice)]
                             (lose state side :credit (:credit runner))
                             (gain state side :credit cost)
                             (system-msg state side (str "loses all credits and gains " cost
                                                         " [Credits] from the rez of " (:title current-ice)))
                             (swap! state update-in [:bonus] dissoc :cost))))}]}

   "NBN: Making News"
   {:recurring 2}

   "NBN: The World is Yours*"
   {:effect (effect (gain :max-hand-size 1))}

   "Near-Earth Hub: Broadcast Center"
   {:events {:server-created {:msg "draw 1 card" :once :per-turn :effect (effect (draw 1))}}}

   "New Angeles Sol: Your News"
   (let [nasol {:optional
                {:prompt "Play a Current?" :player :corp
                 :req (req (not (empty? (filter #(has? % :subtype "Current") (concat (:hand corp) (:discard corp))))))
                 :yes-ability {:prompt "Play a Current from HQ or Archives?" :player :corp
                               :choices ["Archives" "HQ"]
                               :msg (msg "play a Current from " target)
                               :effect (effect (resolve-ability
                                                 {:prompt "Choose a Current to play"
                                                  :choices (req (filter #(and (has? % :subtype "Current")
                                                                              (<= (:cost %) (:credit corp)))
                                                                                ((if (= target "HQ") :hand :discard) corp)))
                                                  :effect (effect (play-instant target))}
                                                card targets))}}}]
     {:events {:agenda-scored nasol :agenda-stolen nasol}})

   "Nisei Division: The Next Generation"
   {:events {:psi-game {:msg "gain 1 [Credits]" :effect (effect (gain :corp :credit 1))}}}

   "Noise: Hacker Extraordinaire"
   {:events {:runner-install {:msg "force the Corp to trash the top card of R&D" :effect (effect (mill :corp))
                              :req (req (has? target :subtype "Virus"))}}}

   "Quetzal: Free Spirit"
   {:abilities [{:once :per-turn :msg "break 1 barrier subroutine"}]}

   "Reina Roja: Freedom Fighter"
   {:effect (effect (gain :link 1))
    :events {:pre-rez {:req (req (and (= (:type target) "ICE") (not (get-in @state [:per-turn (:cid card)]))))
                       :effect (effect (rez-cost-bonus 1))}
             :rez {:req (req (and (= (:type target) "ICE") (not (get-in @state [:per-turn (:cid card)]))))
                              :effect (req (swap! state assoc-in [:per-turn (:cid card)] true))}}}

   "Rielle \"Kit\" Peddler: Transhuman"
   {:abilities [{:req (req (and (:run @state)
                                (:rezzed (get-card state current-ice))))
                 :once :per-turn :msg (msg "make " (:title current-ice) " gain code gate until the end of the run")
                 :effect (req (let [ice current-ice
                                    stypes (:subtype ice)]
                                (update! state side (assoc ice :subtype
                                                               (->> (vec (.split (:subtype ice) " - "))
                                                                    (cons "Code Gate")
                                                                    distinct
                                                                    (join " - "))))
                                (register-events state side {:run-ends
                                                             {:effect (effect (update! (assoc ice :subtype stypes))
                                                                              (unregister-events card))}} card)))}]
    :events {:run-ends nil}}

   "Silhouette: Stealth Operative"
   {:events {:successful-run
             {:req (req (= target :hq)) :once :per-turn
              :effect (effect (resolve-ability {:choices {:req #(= (first (:zone %)) :servers)}
                                                :effect (effect (expose target)) :msg "expose 1 card"}
                                               card nil))}}}

   "Spark Agency: Worldswide Reach"
   {:events
    {:rez {:req (req (has? target :subtype "Advertisement"))
           :once :per-turn
           :effect (effect (lose :runner :credit 1))
           :msg (msg "make the Runner lose 1 [Credits] by rezzing an advertisement")}}}

   "Sunny Lebeau: Security Specialist"
   {:effect (effect (gain :link 2))}

   "SYNC: Everything, Everywhere"
   {:events {:pre-first-turn {:req (req (= side :corp))
                              :effect (effect (update! (assoc card :sync-front true)) (tag-remove-bonus -1))}}
    :abilities [{:cost [:click 1]
                 :effect (req (if (:sync-front card)
                           (do (tag-remove-bonus state side 1)
                               (trash-resource-bonus state side 2)
                               (update! state side (-> card (assoc :sync-front false) (assoc :code "sync"))))
                           (do (tag-remove-bonus state side -1)
                               (trash-resource-bonus state side -2)
                               (update! state side (-> card (assoc :sync-front true)(assoc :code "09001"))))))
                 :msg (msg "flip their ID")}]}

   "Tennin Institute: The Secrets Within"
   {:abilities [{:msg "add 1 advancement counter on a card" :choices {:req #(= (first (:zone %)) :servers)}
                 :req (req (not (:successful-run runner-reg))) :once :per-turn
                 :effect (effect (add-prop target :advance-counter 1))}]}

   "The Foundry: Refining the Process"
   {:events
    {:rez {:req (req (and (= (:type target) "ICE") ;; Did you rez and ice just now
                          (some #(= (:title %) (:title target)) (:deck corp)) ;; Are there more copies in the dec
                          (empty? (let [rezzed-this-turn (map first (turn-events state side :rez))]
                                    (filter #(has? % :type "ICE") rezzed-this-turn))))) ;; Is this the first ice you've rezzed this turn
           :optional
           {:prompt "Add another copy to HQ?"
            :yes-ability {:msg (msg "add a copy of " (:title target) " from R&D to HQ")
                          :effect (effect (move (some #(when (= (:title %) (:title target)) %) (:deck corp)) :hand)
                                          (shuffle! :deck))}}}}}

   "Titan Transnational: Investing In Your Future"
   {:events {:agenda-scored {:msg (msg "add 1 agenda counter to " (:title target))
                             :effect (effect (add-prop target :counter 1))}}}

   "Valencia Estevez: The Angel of Cayambe"
   {:req (req (zero? (get-in @state [:corp :bad-publicity])))
    :effect (effect (gain :corp :bad-publicity 1))}

   "Weyland Consortium: Because We Built It"
   {:recurring 1}

   "Weyland Consortium: Building a Better World"
   {:events {:play-operation {:msg "gain 1 [Credits]" :effect (effect (gain :credit 1))
                              :req (req (has? target :subtype "Transaction"))}}}

   "Whizzard: Master Gamer"
   {:recurring 3}})
