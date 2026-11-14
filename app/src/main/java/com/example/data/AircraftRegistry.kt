package com.example.data

import com.example.model.Aircraft
import com.example.model.AircraftTypeInfo
import com.example.model.CraftCategory
import com.example.model.PublicRegistryData

object AircraftRegistry {

    // Factual Airline & Agency ICAO 3-letter callsign prefixes
    private val AIRLINE_OPERATORS = mapOf(
        "AAL" to "American Airlines",
        "DAL" to "Delta Air Lines",
        "UAL" to "United Airlines",
        "SWA" to "Southwest Airlines",
        "JBU" to "JetBlue Airways",
        "ASA" to "Alaska Airlines",
        "SKW" to "SkyWest Airlines",
        "ENY" to "Envoy Air",
        "JIA" to "PSA Airlines (American Eagle)",
        "PDT" to "Piedmont Airlines (American Eagle)",
        "EDV" to "Endeavor Air (Delta Connection)",
        "RPA" to "Republic Airways",
        "ASH" to "Mesa Airlines",
        "AAY" to "Allegiant Air",
        "NKS" to "Spirit Airlines",
        "FFT" to "Frontier Airlines",
        "SCX" to "Sun Country Airlines",
        "HAL" to "Hawaiian Airlines",
        "FDX" to "FedEx Express",
        "UPS" to "UPS Airlines",
        "ABX" to "ABX Air",
        "ATN" to "Air Transport International",
        "GTI" to "Atlas Air",
        "EJA" to "NetJets Aviation",
        "LXJ" to "Flexjet",
        "VJT" to "VistaJet",
        "WUP" to "Wheels Up",
        "WJA" to "WestJet",
        "ACA" to "Air Canada",
        "BAW" to "British Airways",
        "DLH" to "Lufthansa",
        "AFR" to "Air France",
        "KLM" to "KLM Royal Dutch Airlines",
        "IBE" to "Iberia",
        "SAS" to "Scandinavian Airlines",
        "SWR" to "Swiss International Air Lines",
        "AZA" to "ITA Airways",
        "VIR" to "Virgin Atlantic",
        "UAE" to "Emirates",
        "QTR" to "Qatar Airways",
        "ETD" to "Etihad Airways",
        "ANA" to "All Nippon Airways",
        "JAL" to "Japan Airlines",
        "CPA" to "Cathay Pacific",
        "SIA" to "Singapore Airlines",
        "QFA" to "Qantas Airways",
        "ANZ" to "Air New Zealand",
        "KQA" to "Kenya Airways",
        "ETH" to "Ethiopian Airlines",
        "RCH" to "US Air Force Air Mobility Command",
        "PAT" to "US Army Priority Air Transport",
        "CNV" to "US Navy Fleet Logistics",
        "NASA" to "NASA Flight Operations",
        "JUS" to "US Department of Justice / JPATS"
    )

    // Factual Aircraft Type Specifications Catalog
    val AIRCRAFT_TYPES: Map<String, AircraftTypeInfo> = mapOf(
        "B737" to AircraftTypeInfo(
            typeCode = "B737",
            modelName = "Boeing 737-700",
            manufacturer = "The Boeing Company",
            category = "Narrow-Body Jet Airliner",
            maxSpeedKts = 492,
            cruiseAltitudeFt = 41000,
            rangeNm = 3010,
            wingspanMeters = 35.8,
            passengerCapacity = "128 - 149 Passengers",
            engines = "2x CFM56-7B (24,200 lbf)",
            description = "Next-Generation 737 narrow-body twinjet serving medium-haul commercial airline routes.",
            drawableResName = "plane_boeing_737",
            craftCategory = CraftCategory.JET
        ),
        "B738" to AircraftTypeInfo(
            typeCode = "B738",
            modelName = "Boeing 737-800",
            manufacturer = "The Boeing Company",
            category = "Narrow-Body Jet Airliner",
            maxSpeedKts = 492,
            cruiseAltitudeFt = 41000,
            rangeNm = 2935,
            wingspanMeters = 35.8,
            passengerCapacity = "162 - 189 Passengers",
            engines = "2x CFM International CFM56-7B (27,300 lbf)",
            description = "The Boeing 737-800 is a short-to-medium range twin-engine narrow-body commercial airliner.",
            drawableResName = "plane_boeing_737",
            craftCategory = CraftCategory.JET
        ),
        "B739" to AircraftTypeInfo(
            typeCode = "B739",
            modelName = "Boeing 737-900ER",
            manufacturer = "The Boeing Company",
            category = "Narrow-Body Jet Airliner",
            maxSpeedKts = 492,
            cruiseAltitudeFt = 41000,
            rangeNm = 2950,
            wingspanMeters = 35.8,
            passengerCapacity = "180 - 220 Passengers",
            engines = "2x CFM56-7B26 (26,300 lbf)",
            description = "Stretched Next-Generation Boeing 737 variant with blended winglets.",
            drawableResName = "plane_boeing_737",
            craftCategory = CraftCategory.JET
        ),
        "B38M" to AircraftTypeInfo(
            typeCode = "B38M",
            modelName = "Boeing 737 MAX 8",
            manufacturer = "The Boeing Company",
            category = "Narrow-Body Jet Airliner",
            maxSpeedKts = 492,
            cruiseAltitudeFt = 41000,
            rangeNm = 3550,
            wingspanMeters = 35.9,
            passengerCapacity = "162 - 210 Passengers",
            engines = "2x CFM International LEAP-1B",
            description = "Fourth-generation 737 family narrow-body airliner with split-tip scimitar winglets and LEAP-1B engines.",
            drawableResName = "plane_boeing_737",
            craftCategory = CraftCategory.JET
        ),
        "B39M" to AircraftTypeInfo(
            typeCode = "B39M",
            modelName = "Boeing 737 MAX 9",
            manufacturer = "The Boeing Company",
            category = "Narrow-Body Jet Airliner",
            maxSpeedKts = 492,
            cruiseAltitudeFt = 41000,
            rangeNm = 3550,
            wingspanMeters = 35.9,
            passengerCapacity = "178 - 220 Passengers",
            engines = "2x CFM International LEAP-1B",
            description = "Stretched Boeing 737 MAX series narrow-body jetliner.",
            drawableResName = "plane_boeing_737",
            craftCategory = CraftCategory.JET
        ),
        "B712" to AircraftTypeInfo(
            typeCode = "B712",
            modelName = "Boeing 717-200",
            manufacturer = "The Boeing Company",
            category = "Twin-Engine Narrow-Body Jet",
            maxSpeedKts = 470,
            cruiseAltitudeFt = 37000,
            rangeNm = 1430,
            wingspanMeters = 28.45,
            passengerCapacity = "106 - 117 Passengers",
            engines = "2x Rolls-Royce BR715 turbofans",
            description = "Short-haul twin-engine rear-mounted airliner widely operated by Delta and Hawaiian Airlines.",
            drawableResName = "plane_boeing_737",
            craftCategory = CraftCategory.JET
        ),
        "B752" to AircraftTypeInfo(
            typeCode = "B752",
            modelName = "Boeing 757-200",
            manufacturer = "The Boeing Company",
            category = "Narrow-Body Long-Range Airliner",
            maxSpeedKts = 495,
            cruiseAltitudeFt = 42000,
            rangeNm = 3900,
            wingspanMeters = 38.05,
            passengerCapacity = "200 - 239 Passengers",
            engines = "2x Pratt & Whitney PW2000 or Rolls-Royce RB211",
            description = "High-performance transcontinental and cargo narrow-body twinjet.",
            drawableResName = "plane_boeing_737",
            craftCategory = CraftCategory.JET
        ),
        "B763" to AircraftTypeInfo(
            typeCode = "B763",
            modelName = "Boeing 767-300ER / Freighter",
            manufacturer = "The Boeing Company",
            category = "Wide-Body Jet Airliner / Cargo",
            maxSpeedKts = 492,
            cruiseAltitudeFt = 43100,
            rangeNm = 5980,
            wingspanMeters = 47.57,
            passengerCapacity = "218 - 350 Passengers (or Cargo)",
            engines = "2x GE CF6 / PW4000 / RB211",
            description = "Medium-to-long-range wide-body twinjet operating passenger and express freight corridors.",
            drawableResName = "plane_boeing_777",
            craftCategory = CraftCategory.JET
        ),
        "B772" to AircraftTypeInfo(
            typeCode = "B772",
            modelName = "Boeing 777-200ER",
            manufacturer = "The Boeing Company",
            category = "Wide-Body Long-Range Airliner",
            maxSpeedKts = 495,
            cruiseAltitudeFt = 43100,
            rangeNm = 7065,
            wingspanMeters = 60.9,
            passengerCapacity = "301 - 440 Passengers",
            engines = "2x GE90 / PW4090 / Trent 895",
            description = "Long-range wide-body twinjet commercial airliner.",
            drawableResName = "plane_boeing_777",
            craftCategory = CraftCategory.JET
        ),
        "B77W" to AircraftTypeInfo(
            typeCode = "B77W",
            modelName = "Boeing 777-300ER",
            manufacturer = "The Boeing Company",
            category = "Wide-Body Long-Range Airliner",
            maxSpeedKts = 502,
            cruiseAltitudeFt = 43100,
            rangeNm = 7370,
            wingspanMeters = 64.8,
            passengerCapacity = "396 - 451 Passengers",
            engines = "2x General Electric GE90-115B (115,300 lbf)",
            description = "Long-range wide-body twinjet powered by GE90-115B turbofans.",
            drawableResName = "plane_boeing_777",
            craftCategory = CraftCategory.JET
        ),
        "B788" to AircraftTypeInfo(
            typeCode = "B788",
            modelName = "Boeing 787-8 Dreamliner",
            manufacturer = "The Boeing Company",
            category = "Wide-Body Long-Range Airliner",
            maxSpeedKts = 515,
            cruiseAltitudeFt = 43100,
            rangeNm = 7305,
            wingspanMeters = 60.12,
            passengerCapacity = "242 - 290 Passengers",
            engines = "2x GEnx-1B or Rolls-Royce Trent 1000",
            description = "Composite long-haul twin-aisle airliner.",
            drawableResName = "plane_boeing_777",
            craftCategory = CraftCategory.JET
        ),
        "B789" to AircraftTypeInfo(
            typeCode = "B789",
            modelName = "Boeing 787-9 Dreamliner",
            manufacturer = "The Boeing Company",
            category = "Wide-Body Long-Range Airliner",
            maxSpeedKts = 515,
            cruiseAltitudeFt = 43100,
            rangeNm = 7565,
            wingspanMeters = 60.12,
            passengerCapacity = "290 - 330 Passengers",
            engines = "2x GEnx-1B or Rolls-Royce Trent 1000",
            description = "Stretched composite wide-body twin-engine airliner.",
            drawableResName = "plane_boeing_777",
            craftCategory = CraftCategory.JET
        ),
        "A319" to AircraftTypeInfo(
            typeCode = "A319",
            modelName = "Airbus A319-100",
            manufacturer = "Airbus SE",
            category = "Narrow-Body Jet Airliner",
            maxSpeedKts = 470,
            cruiseAltitudeFt = 39000,
            rangeNm = 3750,
            wingspanMeters = 35.8,
            passengerCapacity = "124 - 156 Passengers",
            engines = "2x CFM56-5B or IAE V2500",
            description = "Shortened fuselage member of the Airbus A320 fly-by-wire family.",
            drawableResName = "plane_airbus_a320",
            craftCategory = CraftCategory.JET
        ),
        "A320" to AircraftTypeInfo(
            typeCode = "A320",
            modelName = "Airbus A320-200",
            manufacturer = "Airbus SE",
            category = "Narrow-Body Jet Airliner",
            maxSpeedKts = 470,
            cruiseAltitudeFt = 39800,
            rangeNm = 3300,
            wingspanMeters = 35.8,
            passengerCapacity = "150 - 180 Passengers",
            engines = "2x CFM56-5B or IAE V2500 turbofans",
            description = "Digital fly-by-wire narrow-body twinjet commercial airliner.",
            drawableResName = "plane_airbus_a320",
            craftCategory = CraftCategory.JET
        ),
        "A20N" to AircraftTypeInfo(
            typeCode = "A20N",
            modelName = "Airbus A320neo",
            manufacturer = "Airbus SE",
            category = "Narrow-Body Jet Airliner",
            maxSpeedKts = 470,
            cruiseAltitudeFt = 39800,
            rangeNm = 3400,
            wingspanMeters = 35.8,
            passengerCapacity = "150 - 194 Passengers",
            engines = "2x CFM LEAP-1A or PW1100G-JM",
            description = "New Engine Option variant of the Airbus A320 with sharklets.",
            drawableResName = "plane_airbus_a320",
            craftCategory = CraftCategory.JET
        ),
        "A321" to AircraftTypeInfo(
            typeCode = "A321",
            modelName = "Airbus A321-200",
            manufacturer = "Airbus SE",
            category = "Narrow-Body Jet Airliner",
            maxSpeedKts = 473,
            cruiseAltitudeFt = 39800,
            rangeNm = 3200,
            wingspanMeters = 35.8,
            passengerCapacity = "185 - 236 Passengers",
            engines = "2x CFM56-5B or IAE V2533",
            description = "Stretched high-capacity narrow-body member of the Airbus A320 family.",
            drawableResName = "plane_airbus_a320",
            craftCategory = CraftCategory.JET
        ),
        "A21N" to AircraftTypeInfo(
            typeCode = "A21N",
            modelName = "Airbus A321neo",
            manufacturer = "Airbus SE",
            category = "Narrow-Body Jet Airliner",
            maxSpeedKts = 473,
            cruiseAltitudeFt = 39800,
            rangeNm = 4000,
            wingspanMeters = 35.8,
            passengerCapacity = "185 - 244 Passengers",
            engines = "2x CFM LEAP-1A or Pratt & Whitney PW1100G",
            description = "Airbus A321neo with high-bypass geared turbofans and extended range.",
            drawableResName = "plane_airbus_a320",
            craftCategory = CraftCategory.JET
        ),
        "A220" to AircraftTypeInfo(
            typeCode = "A220",
            modelName = "Airbus A220-300",
            manufacturer = "Airbus Canada",
            category = "Narrow-Body Jet Airliner",
            maxSpeedKts = 470,
            cruiseAltitudeFt = 41000,
            rangeNm = 3400,
            wingspanMeters = 35.1,
            passengerCapacity = "130 - 160 Passengers",
            engines = "2x Pratt & Whitney PW1500G geared turbofans",
            description = "Clean-sheet narrow-body airliner designed for 100-150 seat mainline routes.",
            drawableResName = "plane_airbus_a320",
            craftCategory = CraftCategory.JET
        ),
        "A333" to AircraftTypeInfo(
            typeCode = "A333",
            modelName = "Airbus A330-300",
            manufacturer = "Airbus SE",
            category = "Wide-Body Long-Range Airliner",
            maxSpeedKts = 490,
            cruiseAltitudeFt = 41100,
            rangeNm = 6350,
            wingspanMeters = 60.3,
            passengerCapacity = "277 - 440 Passengers",
            engines = "2x GE CF6 / PW4000 / Rolls-Royce Trent 700",
            description = "Wide-body twin-engine long-haul passenger and cargo airliner.",
            drawableResName = "plane_airbus_a320",
            craftCategory = CraftCategory.JET
        ),
        "A339" to AircraftTypeInfo(
            typeCode = "A339",
            modelName = "Airbus A330-900neo",
            manufacturer = "Airbus SE",
            category = "Wide-Body Long-Range Airliner",
            maxSpeedKts = 495,
            cruiseAltitudeFt = 41500,
            rangeNm = 7200,
            wingspanMeters = 64.0,
            passengerCapacity = "287 - 460 Passengers",
            engines = "2x Rolls-Royce Trent 7000",
            description = "Re-engined wide-body Airbus A330neo long-range twinjet.",
            drawableResName = "plane_airbus_a320",
            craftCategory = CraftCategory.JET
        ),
        "A359" to AircraftTypeInfo(
            typeCode = "A359",
            modelName = "Airbus A350-900",
            manufacturer = "Airbus SE",
            category = "Wide-Body Long-Range Airliner",
            maxSpeedKts = 510,
            cruiseAltitudeFt = 43100,
            rangeNm = 8300,
            wingspanMeters = 64.75,
            passengerCapacity = "300 - 350 Passengers",
            engines = "2x Rolls-Royce Trent XWB-84",
            description = "Ultra-long-range carbon-fiber wide-body twinjet airliner.",
            drawableResName = "plane_airbus_a320",
            craftCategory = CraftCategory.JET
        ),
        "E145" to AircraftTypeInfo(
            typeCode = "E145",
            modelName = "Embraer ERJ-145",
            manufacturer = "Embraer S.A.",
            category = "Twin-Engine Regional Jet",
            maxSpeedKts = 447,
            cruiseAltitudeFt = 37000,
            rangeNm = 1550,
            wingspanMeters = 20.04,
            passengerCapacity = "50 Passengers",
            engines = "2x Rolls-Royce AE 3007",
            description = "Rear-engined regional jetliner serving commuter routes.",
            drawableResName = "plane_gulfstream_g650",
            craftCategory = CraftCategory.JET
        ),
        "E170" to AircraftTypeInfo(
            typeCode = "E170",
            modelName = "Embraer E170",
            manufacturer = "Embraer S.A.",
            category = "Twin-Engine Regional Passenger Jet",
            maxSpeedKts = 470,
            cruiseAltitudeFt = 41000,
            rangeNm = 2150,
            wingspanMeters = 26.0,
            passengerCapacity = "66 - 78 Passengers",
            engines = "2x General Electric CF34-8E (14,200 lbf)",
            description = "Fly-by-wire regional jet operated widely under mainline regional feeder brands.",
            drawableResName = "plane_boeing_737",
            craftCategory = CraftCategory.JET
        ),
        "E175" to AircraftTypeInfo(
            typeCode = "E175",
            modelName = "Embraer E175",
            manufacturer = "Embraer S.A.",
            category = "Twin-Engine Regional Passenger Jet",
            maxSpeedKts = 470,
            cruiseAltitudeFt = 41000,
            rangeNm = 2200,
            wingspanMeters = 28.65,
            passengerCapacity = "76 - 88 Passengers",
            engines = "2x General Electric CF34-8E (14,200 lbf)",
            description = "Stretched E-Jet regional airliner widely operated across North America.",
            drawableResName = "plane_boeing_737",
            craftCategory = CraftCategory.JET
        ),
        "CRJ2" to AircraftTypeInfo(
            typeCode = "CRJ2",
            modelName = "Bombardier CRJ-200",
            manufacturer = "Bombardier / MHI RJ Aviation",
            category = "Twin-Engine Regional Passenger Jet",
            maxSpeedKts = 464,
            cruiseAltitudeFt = 41000,
            rangeNm = 1700,
            wingspanMeters = 21.21,
            passengerCapacity = "50 Passengers",
            engines = "2x General Electric CF34-3B1",
            description = "50-seat regional jet with rear-mounted engines and T-tail.",
            drawableResName = "plane_gulfstream_g650",
            craftCategory = CraftCategory.JET
        ),
        "CRJ7" to AircraftTypeInfo(
            typeCode = "CRJ7",
            modelName = "Bombardier CRJ-700",
            manufacturer = "Bombardier / MHI RJ Aviation",
            category = "Twin-Engine Regional Passenger Jet",
            maxSpeedKts = 473,
            cruiseAltitudeFt = 41000,
            rangeNm = 1378,
            wingspanMeters = 23.24,
            passengerCapacity = "66 - 78 Passengers",
            engines = "2x General Electric CF34-8C5B1",
            description = "70-seat stretched regional twinjet.",
            drawableResName = "plane_gulfstream_g650",
            craftCategory = CraftCategory.JET
        ),
        "CRJ9" to AircraftTypeInfo(
            typeCode = "CRJ9",
            modelName = "Bombardier CRJ-900",
            manufacturer = "Bombardier / MHI RJ Aviation",
            category = "Twin-Engine Regional Passenger Jet",
            maxSpeedKts = 473,
            cruiseAltitudeFt = 41000,
            rangeNm = 1550,
            wingspanMeters = 24.9,
            passengerCapacity = "76 - 90 Passengers",
            engines = "2x General Electric CF34-8C5 (14,510 lbf)",
            description = "Stretched regional airliner featuring rear-mounted turbofans and T-tail.",
            drawableResName = "plane_gulfstream_g650",
            craftCategory = CraftCategory.JET
        ),
        "GLF6" to AircraftTypeInfo(
            typeCode = "GLF6",
            modelName = "Gulfstream G650ER",
            manufacturer = "Gulfstream Aerospace",
            category = "Ultra Long-Range Business Jet",
            maxSpeedKts = 516,
            cruiseAltitudeFt = 51000,
            rangeNm = 7500,
            wingspanMeters = 30.36,
            passengerCapacity = "11 - 19 Passengers (VIP)",
            engines = "2x Rolls-Royce BR725 A1-12 (16,900 lbf)",
            description = "Ultra-long-range twin-engine executive jetliner.",
            drawableResName = "plane_gulfstream_g650",
            craftCategory = CraftCategory.JET
        ),
        "C56X" to AircraftTypeInfo(
            typeCode = "C56X",
            modelName = "Cessna Citation Excel / XLS",
            manufacturer = "Cessna (Textron Aviation)",
            category = "Midsize Business Jet",
            maxSpeedKts = 441,
            cruiseAltitudeFt = 45000,
            rangeNm = 2100,
            wingspanMeters = 17.17,
            passengerCapacity = "7 - 9 Passengers",
            engines = "2x Pratt & Whitney Canada PW545",
            description = "Popular midsize corporate business jet.",
            drawableResName = "plane_gulfstream_g650",
            craftCategory = CraftCategory.JET
        ),
        "CL35" to AircraftTypeInfo(
            typeCode = "CL35",
            modelName = "Bombardier Challenger 350",
            manufacturer = "Bombardier Aviation",
            category = "Super-Midsize Business Jet",
            maxSpeedKts = 470,
            cruiseAltitudeFt = 45000,
            rangeNm = 3200,
            wingspanMeters = 21.0,
            passengerCapacity = "8 - 10 Passengers",
            engines = "2x Honeywell HTF7350",
            description = "Super-midsize corporate twinjet.",
            drawableResName = "plane_gulfstream_g650",
            craftCategory = CraftCategory.JET
        ),
        "C172" to AircraftTypeInfo(
            typeCode = "C172",
            modelName = "Cessna 172 Skyhawk",
            manufacturer = "Textron Aviation",
            category = "Single-Engine General Aviation",
            maxSpeedKts = 140,
            cruiseAltitudeFt = 14000,
            rangeNm = 640,
            wingspanMeters = 11.0,
            passengerCapacity = "1 Pilot + 3 Passengers",
            engines = "Lycoming IO-360-L2A (180 hp)",
            description = "High-wing, four-seat, single-engine piston general aviation aircraft.",
            drawableResName = "plane_cessna_172",
            craftCategory = CraftCategory.GENERAL_AVIATION
        ),
        "C182" to AircraftTypeInfo(
            typeCode = "C182",
            modelName = "Cessna 182 Skylane",
            manufacturer = "Textron Aviation",
            category = "Single-Engine General Aviation",
            maxSpeedKts = 150,
            cruiseAltitudeFt = 18100,
            rangeNm = 930,
            wingspanMeters = 11.0,
            passengerCapacity = "1 Pilot + 3 Passengers",
            engines = "Lycoming IO-540-AB1A5 (230 hp)",
            description = "High-performance four-seat single-engine piston aircraft.",
            drawableResName = "plane_cessna_172",
            craftCategory = CraftCategory.GENERAL_AVIATION
        ),
        "SR22" to AircraftTypeInfo(
            typeCode = "SR22",
            modelName = "Cirrus SR22",
            manufacturer = "Cirrus Aircraft",
            category = "Single-Engine General Aviation",
            maxSpeedKts = 183,
            cruiseAltitudeFt = 17500,
            rangeNm = 1049,
            wingspanMeters = 11.68,
            passengerCapacity = "1 Pilot + 3-4 Passengers",
            engines = "Continental IO-550-N (310 hp)",
            description = "Low-wing composite four/five-seat piston aircraft equipped with CAPS whole-airframe parachute.",
            drawableResName = "plane_cessna_172",
            craftCategory = CraftCategory.GENERAL_AVIATION
        ),
        "P28A" to AircraftTypeInfo(
            typeCode = "P28A",
            modelName = "Piper PA-28 Cherokee / Archer",
            manufacturer = "Piper Aircraft",
            category = "Single-Engine General Aviation",
            maxSpeedKts = 132,
            cruiseAltitudeFt = 14100,
            rangeNm = 522,
            wingspanMeters = 10.67,
            passengerCapacity = "1 Pilot + 3 Passengers",
            engines = "Lycoming O-360 / IO-360 (180 hp)",
            description = "Low-wing four-seat piston trainer and personal cross-country aircraft.",
            drawableResName = "plane_cessna_172",
            craftCategory = CraftCategory.GENERAL_AVIATION
        ),
        "BE20" to AircraftTypeInfo(
            typeCode = "BE20",
            modelName = "Beechcraft Super King Air 200",
            manufacturer = "Beechcraft (Textron Aviation)",
            category = "Twin-Turboprop Utility & Executive",
            maxSpeedKts = 310,
            cruiseAltitudeFt = 35000,
            rangeNm = 1800,
            wingspanMeters = 16.6,
            passengerCapacity = "1-2 Pilots + 7-9 Passengers",
            engines = "2x Pratt & Whitney Canada PT6A-42 turboprops",
            description = "T-tail pressurized twin-turboprop aircraft.",
            drawableResName = "plane_cessna_172",
            craftCategory = CraftCategory.GENERAL_AVIATION
        ),
        "PC12" to AircraftTypeInfo(
            typeCode = "PC12",
            modelName = "Pilatus PC-12",
            manufacturer = "Pilatus Aircraft",
            category = "Single-Turboprop Utility & Executive",
            maxSpeedKts = 285,
            cruiseAltitudeFt = 30000,
            rangeNm = 1845,
            wingspanMeters = 16.28,
            passengerCapacity = "1-2 Pilots + up to 9 Passengers",
            engines = "Pratt & Whitney Canada PT6A-67P (1,200 shp)",
            description = "Pressurized high-performance single-engine turboprop aircraft.",
            drawableResName = "plane_cessna_172",
            craftCategory = CraftCategory.GENERAL_AVIATION
        ),
        // Helicopters & Rotorcraft Catalog
        "B06" to AircraftTypeInfo(
            typeCode = "B06",
            modelName = "Bell 206 JetRanger",
            manufacturer = "Bell Textron Helicopter",
            category = "Light Single-Turbine Helicopter",
            maxSpeedKts = 120,
            cruiseAltitudeFt = 13500,
            rangeNm = 374,
            wingspanMeters = 10.16,
            passengerCapacity = "1 Pilot + 4 Passengers",
            engines = "Allison 250-C20J Turboshaft (420 shp)",
            description = "Two-bladed light utility and executive turbine helicopter.",
            drawableResName = "plane_bell_206",
            craftCategory = CraftCategory.HELICOPTER
        ),
        "B407" to AircraftTypeInfo(
            typeCode = "B407",
            modelName = "Bell 407",
            manufacturer = "Bell Textron Helicopter",
            category = "Four-Blade Single-Turbine Helicopter",
            maxSpeedKts = 140,
            cruiseAltitudeFt = 18900,
            rangeNm = 324,
            wingspanMeters = 10.67,
            passengerCapacity = "1 Pilot + 5-6 Passengers",
            engines = "Rolls-Royce 250-C47B/E Turboshaft",
            description = "Four-blade single-engine utility helicopter widely used in law enforcement, EMS, and charter operations.",
            drawableResName = "plane_bell_206",
            craftCategory = CraftCategory.HELICOPTER
        ),
        "B412" to AircraftTypeInfo(
            typeCode = "B412",
            modelName = "Bell 412",
            manufacturer = "Bell Textron Helicopter",
            category = "Twin-Turbine Medium Helicopter",
            maxSpeedKts = 140,
            cruiseAltitudeFt = 20000,
            rangeNm = 358,
            wingspanMeters = 14.02,
            passengerCapacity = "2 Pilots + up to 13 Passengers",
            engines = "2x Pratt & Whitney Canada PT6T Twin-Pac",
            description = "Twin-engine utility and aeromedical transport helicopter.",
            drawableResName = "plane_bell_206",
            craftCategory = CraftCategory.HELICOPTER
        ),
        "EC35" to AircraftTypeInfo(
            typeCode = "EC35",
            modelName = "Airbus Helicopters H135 (EC135)",
            manufacturer = "Airbus Helicopters",
            category = "Twin-Engine Light Utility Helicopter",
            maxSpeedKts = 137,
            cruiseAltitudeFt = 15000,
            rangeNm = 342,
            wingspanMeters = 10.20,
            passengerCapacity = "1-2 Pilots + 5 Passengers / Medical Crew",
            engines = "2x PW206B or Turbomeca Arrius 2B2",
            description = "Twin-engine helicopter with Fenestron enclosed tail rotor used in air medical and public safety missions.",
            drawableResName = "plane_bell_206",
            craftCategory = CraftCategory.HELICOPTER
        ),
        "EC45" to AircraftTypeInfo(
            typeCode = "EC45",
            modelName = "Airbus Helicopters H145 (EC145)",
            manufacturer = "Airbus Helicopters",
            category = "Twin-Turbine Utility & Medevac Helicopter",
            maxSpeedKts = 145,
            cruiseAltitudeFt = 18000,
            rangeNm = 370,
            wingspanMeters = 11.0,
            passengerCapacity = "1-2 Crew + up to 8-9 Passengers",
            engines = "2x Safran Arriel 1E2 / 2E Turboshafts",
            description = "Twin-engine medium-light rotorcraft widely deployed in HEMS, SAR, and law enforcement.",
            drawableResName = "plane_bell_206",
            craftCategory = CraftCategory.HELICOPTER
        ),
        "AS50" to AircraftTypeInfo(
            typeCode = "AS50",
            modelName = "Airbus Helicopters H125 (AS350 AStar)",
            manufacturer = "Airbus Helicopters",
            category = "Light Single-Turbine Helicopter",
            maxSpeedKts = 155,
            cruiseAltitudeFt = 23000,
            rangeNm = 340,
            wingspanMeters = 10.69,
            passengerCapacity = "1 Pilot + 5-6 Passengers",
            engines = "Safran Arriel 2D Turboshaft",
            description = "High-altitude single-engine light utility helicopter.",
            drawableResName = "plane_bell_206",
            craftCategory = CraftCategory.HELICOPTER
        ),
        "R44" to AircraftTypeInfo(
            typeCode = "R44",
            modelName = "Robinson R44 Raven II",
            manufacturer = "Robinson Helicopter Company",
            category = "Four-Seat Light Piston Helicopter",
            maxSpeedKts = 110,
            cruiseAltitudeFt = 14000,
            rangeNm = 300,
            wingspanMeters = 10.06,
            passengerCapacity = "1 Pilot + 3 Passengers",
            engines = "Lycoming IO-540-AE1A5 piston (245 hp)",
            description = "Four-place light piston helicopter used for training, utility, and personal transport.",
            drawableResName = "plane_bell_206",
            craftCategory = CraftCategory.HELICOPTER
        ),
        "R66" to AircraftTypeInfo(
            typeCode = "R66",
            modelName = "Robinson R66 Turbine",
            manufacturer = "Robinson Helicopter Company",
            category = "Five-Seat Light Turbine Helicopter",
            maxSpeedKts = 120,
            cruiseAltitudeFt = 14000,
            rangeNm = 350,
            wingspanMeters = 10.06,
            passengerCapacity = "1 Pilot + 4 Passengers",
            engines = "Rolls-Royce RR300 Turboshaft (300 shp)",
            description = "Five-seat single-turbine helicopter.",
            drawableResName = "plane_bell_206",
            craftCategory = CraftCategory.HELICOPTER
        ),
        "S76" to AircraftTypeInfo(
            typeCode = "S76",
            modelName = "Sikorsky S-76",
            manufacturer = "Sikorsky Aircraft",
            category = "Twin-Turbine Medium Helicopter",
            maxSpeedKts = 155,
            cruiseAltitudeFt = 13800,
            rangeNm = 411,
            wingspanMeters = 13.41,
            passengerCapacity = "2 Pilots + up to 12 Passengers",
            engines = "2x Pratt & Whitney PW210S or Arriel 2S2",
            description = "Medium twin-engine executive, offshore, and EMS helicopter.",
            drawableResName = "plane_bell_206",
            craftCategory = CraftCategory.HELICOPTER
        ),
        "UH60" to AircraftTypeInfo(
            typeCode = "UH60",
            modelName = "Sikorsky UH-60 Black Hawk",
            manufacturer = "Sikorsky Aircraft (Lockheed Martin)",
            category = "Twin-Turbine Tactical Helicopter",
            maxSpeedKts = 159,
            cruiseAltitudeFt = 19000,
            rangeNm = 314,
            wingspanMeters = 16.36,
            passengerCapacity = "4 Crew + 11 Personnel",
            engines = "2x General Electric T700-GE-701D",
            description = "Four-blade, twin-engine medium-lift tactical utility helicopter.",
            drawableResName = "plane_black_hawk",
            craftCategory = CraftCategory.HELICOPTER
        ),
        "F16" to AircraftTypeInfo(
            typeCode = "F16",
            modelName = "General Dynamics F-16 Fighting Falcon",
            manufacturer = "Lockheed Martin",
            category = "Supersonic Multirole Military Fighter",
            maxSpeedKts = 1147,
            cruiseAltitudeFt = 50000,
            rangeNm = 2277,
            wingspanMeters = 9.96,
            passengerCapacity = "1 Pilot",
            engines = "Pratt & Whitney F100-PW-229 afterburning turbofan",
            description = "Supersonic single-engine multirole tactical fighter aircraft.",
            drawableResName = "plane_gulfstream_g650",
            craftCategory = CraftCategory.MILITARY
        )
    )

    /**
     * Resolves an authentic AircraftTypeInfo from an ICAO type code and optional ADS-B description.
     * NEVER substitutes an unknown code with Boeing 737 or Cessna 172!
     */
    fun getAircraftType(
        code: String,
        descriptionHint: String = "",
        isHelicopterHint: Boolean = false
    ): AircraftTypeInfo {
        val clean = code.trim().uppercase()
        val mappedCode = when (clean) {
            "EC145", "H145", "BK17", "BK117", "BK117C2" -> "EC45"
            "BELL407", "407", "B07" -> "B407"
            "BELL412", "412", "B12" -> "B412"
            "BELL206", "206", "B206", "B06T" -> "B06"
            "H135", "EC135" -> "EC35"
            "H125", "AS350" -> "AS50"
            "BLACKHAWK", "H60", "S70" -> "UH60"
            "E75L", "E75S" -> "E175"
            "E70L" -> "E170"
            "BCS1", "BCS3" -> "A220"
            "PA28", "P28R", "P28B" -> "P28A"
            else -> clean
        }

        AIRCRAFT_TYPES[mappedCode]?.let { return it }

        val isRotor = isHelicopterHint || clean in Aircraft.ROTORCRAFT_ICAO_TYPES ||
                descriptionHint.contains("helicopter", ignoreCase = true) ||
                descriptionHint.contains("rotor", ignoreCase = true)

        val inferredCategory = when {
            isRotor -> CraftCategory.HELICOPTER
            clean.startsWith("A3") || clean.startsWith("A2") || clean.startsWith("B7") ||
                    clean.startsWith("B3") || clean.startsWith("E1") || clean.startsWith("CRJ") ||
                    clean.startsWith("GLF") || clean.startsWith("C5") || clean.startsWith("CL") -> CraftCategory.JET
            else -> CraftCategory.AIRPLANE
        }

        val inferredPhoto = when {
            isRotor -> if (clean == "UH60" || clean == "H60") "plane_black_hawk" else "plane_bell_206"
            clean.startsWith("A3") || clean.startsWith("A2") || clean.startsWith("BCS") -> "plane_airbus_a320"
            clean.startsWith("B77") || clean.startsWith("B78") || clean.startsWith("B74") || clean.startsWith("B76") -> "plane_boeing_777"
            clean.startsWith("B7") || clean.startsWith("B3") -> "plane_boeing_737"
            clean in listOf("C152", "C172", "C182", "C206", "SR20", "SR22", "P28A", "PA28") -> "plane_cessna_172"
            else -> null // Do not guess or mismatch photos for uncataloged rare models
        }

        val displayModel = descriptionHint.ifBlank {
            if (clean.isNotBlank()) "ICAO $clean" else if (isRotor) "Helicopter (Unlisted Type)" else "Aircraft (Type Pending)"
        }

        val inferredMfg = when {
            descriptionHint.contains("Airbus", ignoreCase = true) || clean.startsWith("A3") || clean.startsWith("A2") -> "Airbus"
            descriptionHint.contains("Boeing", ignoreCase = true) || clean.startsWith("B7") || clean.startsWith("B3") -> "Boeing"
            descriptionHint.contains("Bell", ignoreCase = true) || clean in listOf("B06", "B407", "B412", "B429", "B505") -> "Bell Textron"
            descriptionHint.contains("Cessna", ignoreCase = true) -> "Textron Aviation / Cessna"
            descriptionHint.contains("Piper", ignoreCase = true) -> "Piper Aircraft"
            descriptionHint.contains("Cirrus", ignoreCase = true) -> "Cirrus Aircraft"
            descriptionHint.contains("Embraer", ignoreCase = true) || clean.startsWith("E1") || clean.startsWith("E7") -> "Embraer"
            descriptionHint.contains("Bombardier", ignoreCase = true) || clean.startsWith("CRJ") || clean.startsWith("CL") -> "Bombardier"
            descriptionHint.contains("Sikorsky", ignoreCase = true) -> "Sikorsky Aircraft"
            descriptionHint.contains("Robinson", ignoreCase = true) -> "Robinson Helicopter"
            else -> "Pending Registry Lookup"
        }

        return AircraftTypeInfo(
            typeCode = clean.ifBlank { "UNK" },
            modelName = displayModel,
            manufacturer = inferredMfg,
            category = if (isRotor) "Helicopter / Rotorcraft" else "ADS-B Verified Aircraft",
            maxSpeedKts = 0,
            cruiseAltitudeFt = 0,
            rangeNm = 0,
            wingspanMeters = 0.0,
            passengerCapacity = "See FAA/ICAO Type $clean",
            engines = "See Manufacturer Registry",
            description = if (clean.isNotBlank()) {
                "Live ADS-B transponder airframe type $clean${if (descriptionHint.isNotBlank()) " ($descriptionHint)" else ""}."
            } else {
                "Transponder is broadcasting live position without an ICAO type code in the initial frame."
            },
            drawableResName = inferredPhoto,
            craftCategory = inferredCategory
        )
    }

    /**
     * Resolves airline operator strictly from known ICAO 3-letter callsign prefixes
     * or registration jurisdiction — never fabricates private owners or companies.
     */
    fun resolveRegisteredOperator(callsign: String, tailNumber: String, icao24: String): String {
        val cleanCallsign = callsign.trim().uppercase()
        if (cleanCallsign.length >= 3) {
            val prefix = cleanCallsign.substring(0, 3)
            AIRLINE_OPERATORS[prefix]?.let { return it }
        }

        val cleanTail = tailNumber.trim().uppercase()
        val cleanHex = icao24.trim().lowercase()

        return when {
            cleanCallsign.startsWith("EJA") -> "NetJets Aviation"
            cleanCallsign.startsWith("LXJ") -> "Flexjet"
            cleanCallsign.startsWith("VJT") -> "VistaJet"
            cleanCallsign.startsWith("WUP") -> "Wheels Up"
            cleanCallsign.startsWith("RCH") -> "United States Air Force"
            cleanCallsign.startsWith("CNV") -> "United States Navy"
            cleanCallsign.startsWith("PAT") -> "United States Army"
            cleanTail.startsWith("N") || cleanHex.startsWith("a") -> "US Civil Registration (Owner Unlisted)"
            cleanTail.startsWith("G-") -> "UK Civil Registration"
            cleanTail.startsWith("C-") -> "Canadian Civil Registration"
            cleanTail.startsWith("D-") -> "German Civil Registration"
            cleanTail.startsWith("F-") -> "French Civil Registration"
            cleanTail.startsWith("VH-") -> "Australian Civil Registration"
            cleanTail.startsWith("JA") -> "Japanese Civil Registration"
            else -> "Unlisted Operator"
        }
    }

    /**
     * Builds truthful PublicRegistryData from live ADS-B / ADSBDB owner metadata without fake dates or serial numbers.
     */
    fun resolvePublicRegistry(
        tailNumber: String,
        callsign: String,
        icao24: String,
        operator: String
    ): PublicRegistryData {
        val cleanTail = tailNumber.trim().uppercase()
        val cleanHex = icao24.trim().uppercase()
        val isUs = cleanTail.startsWith("N") || cleanHex.startsWith("A")
        val cleanOwner = operator.ifBlank { resolveRegisteredOperator(callsign, cleanTail, cleanHex) }

        return PublicRegistryData(
            tailNumber = cleanTail,
            icao24 = cleanHex,
            registeredOwner = cleanOwner,
            registeredParty = if (cleanTail.isNotBlank()) "Registry Record #$cleanTail" else "ICAO Hex #$cleanHex",
            representativeTitle = "ADS-B / Civil Registry",
            partyClassification = if (isUs) "FAA Civil Registry" else "ICAO Registry",
            registryAuthority = if (isUs) "Federal Aviation Administration (FAA)" else "Civil Aviation Authority (ICAO)",
            registryCountry = if (isUs) "United States" else "International",
            registryStateOrProvince = null,
            certificateStatus = "Active Transponder",
            airworthinessClass = "Verified Live ADS-B Contact",
            serialNumber = "",
            issueDate = "",
            expirationDate = ""
        )
    }

    private const val CHARSET = "ABCDEFGHJKLMNPQRSTUVWXYZ" // FAA alphabet (no I or O)
    private const val DIGITSET = "0123456789"
    private const val ALLCHARS = CHARSET + DIGITSET

    private const val SUFFIX_SIZE = 1 + 24 * (1 + 24) // 601
    private const val BUCKET4_SIZE = 1 + 24 + 10 // 35
    private const val BUCKET3_SIZE = 10 * BUCKET4_SIZE + SUFFIX_SIZE // 951
    private const val BUCKET2_SIZE = 10 * BUCKET3_SIZE + SUFFIX_SIZE // 10111
    private const val BUCKET1_SIZE = 10 * BUCKET2_SIZE + SUFFIX_SIZE // 101711

    private fun getSuffix(offset: Int): String {
        if (offset == 0) return ""
        val char0 = CHARSET[(offset - 1) / (CHARSET.length + 1)]
        val rem = (offset - 1) % (CHARSET.length + 1)
        return if (rem == 0) char0.toString() else "$char0${CHARSET[rem - 1]}"
    }

    /**
     * Converts a US ICAO hex address (A00001 - ADF7C7) into an FAA N-Number registration
     * using the official FAA Mode-S mathematical algorithm.
     */
    fun convertIcao24ToTailNumber(icao24: String): String {
        val hex = icao24.trim().lowercase()
        val hexInt = hex.toIntOrNull(16) ?: return hex.uppercase()

        if (hexInt in 0xA00001..0xADF7C7) {
            val i = hexInt - 0xA00001
            val dig1 = i / BUCKET1_SIZE + 1
            var rem1 = i % BUCKET1_SIZE
            val out = StringBuilder("N$dig1")
            if (rem1 < SUFFIX_SIZE) {
                return out.append(getSuffix(rem1)).toString()
            }
            rem1 -= SUFFIX_SIZE
            val dig2 = rem1 / BUCKET2_SIZE
            var rem2 = rem1 % BUCKET2_SIZE
            out.append(dig2)
            if (rem2 < SUFFIX_SIZE) {
                return out.append(getSuffix(rem2)).toString()
            }
            rem2 -= SUFFIX_SIZE
            val dig3 = rem2 / BUCKET3_SIZE
            var rem3 = rem2 % BUCKET3_SIZE
            out.append(dig3)
            if (rem3 < SUFFIX_SIZE) {
                return out.append(getSuffix(rem3)).toString()
            }
            rem3 -= SUFFIX_SIZE
            val dig4 = rem3 / BUCKET4_SIZE
            val rem4 = rem3 % BUCKET4_SIZE
            out.append(dig4)
            if (rem4 == 0) {
                return out.toString()
            }
            return out.append(ALLCHARS[rem4 - 1]).toString()
        }

        return when {
            hex.startsWith("40") || hex.startsWith("41") || hex.startsWith("42") || hex.startsWith("43") -> "G-" + hex.takeLast(4).uppercase()
            hex.startsWith("c0") || hex.startsWith("c1") || hex.startsWith("c2") || hex.startsWith("c3") -> "C-" + hex.takeLast(4).uppercase()
            hex.startsWith("3c") || hex.startsWith("3d") || hex.startsWith("3e") || hex.startsWith("3f") -> "D-" + hex.takeLast(4).uppercase()
            hex.startsWith("38") || hex.startsWith("39") || hex.startsWith("3a") || hex.startsWith("3b") -> "F-" + hex.takeLast(4).uppercase()
            hex.startsWith("7c") || hex.startsWith("7d") -> "VH-" + hex.takeLast(3).uppercase()
            hex.startsWith("84") || hex.startsWith("85") || hex.startsWith("86") -> "JA" + hex.takeLast(4).uppercase()
            else -> hex.uppercase()
        }
    }
}
