#!/bin/bash
#ROW FORMAT DELIMITED FIELDS TERMINATED BY "|"
#partitioned by (shopid string)

YEAR=$1
MONTH=$2
DAY=$3

argc=$#

if [ $argc -ne 3 ]
then
  echo "Incorrect number of parameters."
  echo " expected are: "
  echo "   $0   [YEAR] [MONTH] [DAY]"
  echo ""
  echo " example: "
  echo "   $0   2015 09 11"
  echo ""
  exit 1
fi

eval $(hive -e "create table IF NOT EXISTS SMAv2OutputPOC1991(shopid String, posPCC String, shopDateStr String, origAirportCd String, destAirportCd String, outDepDate String, inDepDate String,
totalPaxCount String, itineraryCount String, airlineCd String, itineraryseqnbr String, carriershare String, roboticshopindicator String)
ROW FORMAT DELIMITED FIELDS TERMINATED BY '|';
INSERT OVERWRITE TABLE SMAv2OutputPOC1991
select
IF(f2.shopid <=> NULL, f1.shopid , f2.shopid) as shopid, f2.pospcc as pospcc, from_unixtime(CAST(CAST(f2.shopdatestr as BIGINT)/1000 as BIGINT)) as shopdatestr, f2.origairportcd as origairportcd,
f2.destairportcd as destairportcd, f2.outdepdate as outdepdate, f2.indepdate as indepdate, f2.totalpaxcount as totalpaxcount, f2.total as itineraryCount,
f2.airlinecd as airlineCd, f2.itineraryseqnbr as itineraryseqnbr, f2.carriershare as carriershare, f1.roboticshopindicator as roboticshopindicator
from
(select
shopid, roboticshopindicator
from
roboticshopping
where
year = '${YEAR}' AND month = '${MONTH}' AND day = '${DAY}') f1
FULL OUTER JOIN
(SELECT
 O1.shopid, O1.airlineCd, O1.itinerarySeqNbr, O1.carrierShare, O2.posPCC,
 O2.shopDateStr, O2.origAirportCd, O2.destAirportCd, O2.outDepDate, O2.inDepDate, O2.totalPaxCount, O2.total
FROM 
(select q1.shopid as shopid, q1.marketingairlinecode as airlineCd, cast(q1.itineraryseqnbr as string) as itinerarySeqNbr, cast(q1.count as string) as carrierShare
from
(select t2.shopid, t2.flights, t2.flights[0] as q1c2, t2.flights[0].marketingairlinecode,
size(t2.flights) as count, t2.itineraryseqnbr , exploded_table.seq as seqnbr, exploded_table.f as flightselement
from
(select shopid, f.flights as flights, f.itineraryseqnbr as itineraryseqnbr
from shoprecord
lateral view explode(responseitineraries) exploded_table as f
where year = '${YEAR}' and month = '${MONTH}' and day = '${DAY}') t2
lateral view posexplode(t2.flights)
exploded_table as seq, f) q1
where q1.marketingairlinecode = q1.flightselement.marketingairlinecode AND q1.seqnbr = 0) O1
LEFT OUTER JOIN 
(select shoprecord.shopid as shopid,
 request.pospcc as posPCC,  
 request.systemtimestamp as shopDateStr, 
 request.origdest[0].origairportcode as origAirportCd,
 request.origdest[0].destairportcode as destAirportCd,
 request.origdest[0].departuredatetime as outDepDate,
 request.origdest[1].departuredatetime as inDepDate,
 cast(passengertable.Totalpaxcount as string) as totalPaxCount,
 cast(size(responseitineraries) as string) as total
from
shoprecord LEFT JOIN (select shopid, sum(t1.paxquantity) as Totalpaxcount
from shoprecord
LATERAL VIEW explode(request.passenger)
exploded_table as t1
where year = '${YEAR}' AND month = '${MONTH}' AND day = '${DAY}'
GROUP BY shopid) passengertable on passengertable.shopid =  shoprecord.shopid
where year = '${YEAR}' and month = '${MONTH}' and day = '${DAY}') O2
ON (O1.shopid = O2.shopid)) f2
ON (f1.shopid = f2.shopid)
ORDER BY
roboticshopindicator, shopid, posPCC, shopDateStr, origAirportCd, destAirportCd, outDepDate, inDepDate,
totalPaxCount, itineraryCount, airlineCd, itineraryseqnbr, carriershare;")

EXIT_CODE=$?

if [ $EXIT_CODE -eq 0 ]; then
    echo "OK"
else
    echo "FAIL"
    echo "For input params: ${YEAR}/${MONTH}/${DAY}"
fi
