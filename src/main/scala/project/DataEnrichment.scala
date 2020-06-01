package project

import java.io._

import scala.io.Source

object DataEnrichment extends App {

  val bufferedLocation = "C:/Users/ganesha/Desktop/gtfs_stm/"
  val bufferedSourceTrips = Source.fromFile(bufferedLocation + "trips.txt")
  val tripList: List[Trip] = bufferedSourceTrips
    .getLines()
    .toList
    .tail
    .map(_.split(",", -1))
    .map(n => Trip(n(0).toInt, n(1), n(2), n(3), n(4).toInt, n(5).toInt, n(6).toInt,
      if (n(7).isEmpty) None else Some(n(7)),
      if (n(8).isEmpty) None else Some(n(8))))
  bufferedSourceTrips.close


  val bufferedSourceRoute = Source.fromFile(bufferedLocation + "routes.txt")
  val routeList: List[Route] = bufferedSourceRoute
    .getLines()
    .toList
    .tail
    .map(_.split(",", -1))
    .map(n => Route(n(0).toInt, n(1), n(2), n(3), n(4).toInt, n(5), n(6), n(7)))
    .filter(_.routeType == 1)
  bufferedSourceRoute.close


  val bufferedSourceCalendar = Source.fromFile(bufferedLocation + "calendar.txt")
  val calendarList: List[Calendar] = bufferedSourceCalendar
    .getLines()
    .toList
    .tail
    .map(_.split(",", -1))
    .map(n => Calendar(n(0), n(1).toInt, n(2).toInt, n(3).toInt, n(4).toInt, n(5).toInt, n(6).toInt, n(7).toInt, n(8), n(9)))
    .filter(_.monday == 1)
  bufferedSourceCalendar.close


  val routeMap: RouteLookup = new RouteLookup(routeList)
  val routeTrips: List[RouteTrip] =
    tripList.map(line => RouteTrip(line, routeMap.lookup(line.routeId)))
      .filter(_.route != null)


  val enrichedTrips: List[JoinOutput] =
    new GenericNestedLoopJoin[RouteTrip, Calendar]((i, j) => i.trip.serviceId == j.serviceId)
      .join(routeTrips, calendarList)


  val outDataLines: List[String] =
    enrichedTrips
      .map(n =>
        EnrichedTrip.formatOutput(n.left.asInstanceOf[RouteTrip].trip,
          n.left.asInstanceOf[RouteTrip].route,
          n.right.asInstanceOf[Calendar]))


  val outFile = new File(bufferedLocation + "trp.csv")
  val bw = new BufferedWriter(new FileWriter(outFile))

  val l1 = List("route_id", "service_id", "trip_id", "trip_headsign",
    "direction_id", "shape_id", "wheelchair_accessible",
    "note_fr", "note_en", "route_long_name")
  for (line <- l1) {
    bw.write(line + ",")
  }

  for (line <- outDataLines) {
    bw.newLine()
    bw.write(line)
  }

  bw.close()

  enrichedTrips foreach println

  println()
  println(s"Data enrichment complete.  ${outDataLines.size} records written to file SubwayTrips.csv")

}