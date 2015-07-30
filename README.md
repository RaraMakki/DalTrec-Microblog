# DalTrec-Microblog

This code reads the crawled tweets from json files in ./data/tweets and index them separately for each day. Then, loads the manually assigned weights from ./data/ManuallyLabeledKeyterms2.txt. These weights indicate the importance of manually extracted keyterms in each topic/profile. Using these weights, we create boosted queries and set Lucene similarity to "LMDirichletSimilarity". The ranked tweets returned by Lucene are tested to see whether the sum of the weights of the keyterms occurred in each tweet passes a predefined threshold or not. If yes, then the tweet is returned as relevant. Otherwise, it is ignored. 

To run, first clone the repo and then build it with maven "mvn clean package appassembler:assemble". Note, that the json files of the crawled tweets should be available in "./data/tweets/".
