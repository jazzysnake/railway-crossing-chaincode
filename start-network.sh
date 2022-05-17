#!/bin/sh
export MICROFAB_CONFIG='{
    "endorsing_organizations":[
        {
            "name": "RailwayOrg"
        },
        {
            "name": "VehicleOwnerOrg"
        }
    ],
    "channels":[
        {
            "name": "railway-channel",
            "endorsing_organizations":[
                "RailwayOrg",
                "VehicleOwnerOrg"
            ]
        }
    ]
}'

docker run -p 8080:8080 --rm -e MICROFAB_CONFIG ibmcom/ibp-microfab