package io.bunkr;

import com.mongodb.*;
import org.bson.types.ObjectId;
import org.geojson.Point;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import javax.annotation.PostConstruct;
import static org.springframework.web.bind.annotation.RequestMethod.GET;

/**
 * Created by mroxb on 1/4/2016.
 */
@RestController
@RequestMapping("/mongoclient")
public class CityControllerMongoClient {
    final DB db;

    def dbObjectToCityTransformer = { DBObject it ->
            def objectMap = it.toMap();
            return new City(_id: objectMap._id, name: objectMap.name, location: new Point(objectMap.location.coordinates[0], objectMap.location.coordinates[1]))
    }

    @Autowired
    CityControllerMongoClient(MongoClient mongoClient) {
        db = mongoClient.getDB("citydbmongoclient");
    }

    @RequestMapping(value="/", method = GET)
    List<City> index() {
        return db.getCollection("city").find().collect(dbObjectToCityTransformer);
    }

    @RequestMapping(value="/near/{cityName}", method = GET)
    ResponseEntity nearCity(@PathVariable String cityName) {
        def city = dbObjectToCityTransformer(db.getCollection("city").findOne(new BasicDBObject("name", cityName)))
        if(city) {
            def point = new BasicDBObject([type: "Point", coordinates: [city.location.coordinates.longitude, city.location.coordinates.latitude]])
            def geoNearCommand =  new BasicDBObject([geoNear: "city", spherical: true, near: point])
            def closestCities = db.command(geoNearCommand).toMap()
            def closest = closestCities.results[1]
            return new ResponseEntity([name:closest.obj.name, distance:closest.dis/1000], HttpStatus.OK)
        }
        else {
            return new ResponseEntity(HttpStatus.NOT_FOUND)
        }
    }

    @PostConstruct
    void populateCities() {
        db.getCollection("city").drop()
        [new City(name: "London",
                location: new Point(-0.125487, 51.508515)),
         new City(name: "Paris",
                 location: new Point(2.352222, 48.856614)),
         new City(name: "New York",
                 location: new Point(-74.005973, 40.714353)),
         new City(name: "San Francisco",
                 location: new Point(-122.419416, 37.774929))].each {
            DBObject location = new BasicDBObject([type: "Point", coordinates: [it.location.coordinates.longitude, it.location.coordinates.latitude]])
            DBObject city = new BasicDBObject([name: it.name, location: location])
            db.getCollection("city").insert(city)
        }
        db.getCollection("city").createIndex(new BasicDBObject("location", "2dsphere"))
    }

    static class City {
        ObjectId _id;
        String name;
        Point location;
    }
}
