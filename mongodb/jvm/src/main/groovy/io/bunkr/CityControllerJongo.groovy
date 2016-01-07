package io.bunkr

import com.fasterxml.jackson.databind.ObjectMapper
import com.mongodb.MongoClient
import org.bson.types.ObjectId
import org.geojson.Point
import org.jongo.Jongo
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.*
import org.springframework.web.bind.annotation.*

import javax.annotation.PostConstruct

import static org.springframework.web.bind.annotation.RequestMethod.GET

@RestController
@RequestMapping("/jongo")
class CityControllerJongo {

    final Jongo jongo

    @Autowired
    CityControllerJongo(MongoClient mongoClient) {
        jongo = new Jongo(mongoClient.getDB("citydbjongo"))
    }

    @RequestMapping(value="/", method = GET)
    List<City> index() {
        return jongo.getCollection("city").find().as(City).asList()
    }

    @RequestMapping(value="/near/{cityName}", method = GET)
    ResponseEntity nearCity(@PathVariable String cityName) {
        def city = jongo.getCollection("city").findOne("{name:'$cityName'}").as(City)
        if(city) {
            def command = """{
                geoNear: "city",
                near: ${new ObjectMapper().writeValueAsString(city.location)},
                spherical: true
            }"""
            def closestCities = jongo.runCommand(command).as(GeoNearResult) as GeoNearResult<City>
            def closest = closestCities.results[1]
            return new ResponseEntity([name:closest.obj.name, distance:closest.dis/1000], HttpStatus.OK)
        }
        else {
            return new ResponseEntity(HttpStatus.NOT_FOUND)
        }
    }

    @PostConstruct
    void populateCities() {
        jongo.getCollection("city").drop()
        [ new City( name:"London",
                location: new Point(-0.125487, 51.508515)),
          new City( name:"Paris",
                  location: new Point(2.352222, 48.856614)),
          new City( name:"New York",
                  location: new Point(-74.005973, 40.714353)),
          new City( name:"San Francisco",
                  location: new Point(-122.419416, 37.774929)) ].each {
            jongo.getCollection("city").save(it)
        }
        jongo.getCollection("city").ensureIndex("{location:'2dsphere'}")
    }

    static class GeoNearResult<O> {
        List<GeoNearItem<O>> results
    }

    static class GeoNearItem<O> {
        Double dis
        O obj
    }

    static class City {
        ObjectId _id
        String name
        Point location
    }
}