val f = sc.textFile("hdfs://10.118.19.247:8020/data/voters.csv")

val voters = f.map { x =>
  val (c, n) = x.span(x => x != ',')
  (c, n.tail)
}.cache()

// how many voters?
voters.count()

// /!\ crash the who thing => seq tooooo long
//___________________________________________
// voters by country
//val byCountry = voters.groupByKey()
// number in BE
// val bes = byCountry.lookup("BE").size

voters.countByKeyApprox(3000).onComplete(m => println(m("BE")))

/*___________________________________________*/

val f2 = sc.textFile("hdfs://10.118.19.247:8020/data/votes.csv")

val votes = f2.map { x =>
  val (c, n) = x.span(x => x != ',')
  (c, n.tail)
}.cache()

val partitesCount = for {
  p <- Seq("X", "Y", "Z")
} yield (p -> votes.filter(_._1 == p).count())

// how many distinct "ones"
val ones = votes.distinct().collect()

// who won?
val onesCount = for {
  one     <- ones
} yield (one -> votes.filter(_ == one).count())

onesCount.maxBy(_._2)