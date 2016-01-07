package io.bunkr

import com.mongodb.*
import org.bson.types.ObjectId
import org.geojson.Point
import org.mongodb.morphia.*
import org.mongodb.morphia.annotations.*
import org.mongodb.morphia.converters.TypeConverter
import org.mongodb.morphia.mapping.MappedField
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.*
import org.springframework.web.bind.annotation.*

import javax.annotation.PostConstruct

import static org.springframework.web.bind.annotation.RequestMethod.GET

@RestController
@RequestMapping("/mongomorphia")
class CityControllerMorphia {
    final Datastore datastore

    @Autowired
    CityControllerMorphia(MongoClient mongoClient) {
        def morphia = new Morphia()
        morphia.mapper.converters.addConverter(GeoJsonPointTypeConverter)
        datastore = morphia.createDatastore(mongoClient, "citymorphia")
    }

    @RequestMapping(value="/", method = GET)
    List<City> index() {
        return datastore.find(City).asList()
    }

    @RequestMapping(value="/near/{cityName}", method = GET)
    ResponseEntity nearCity(@PathVariable String cityName) {
        def city = datastore.find(City, "name", cityName).get()
        if(city) {
            def point = new BasicDBObject([type: "Point", coordinates: [city.location.coordinates.longitude, city.location.coordinates.latitude]])
            def geoNearCommand =  new BasicDBObject([geoNear: "City", spherical: true, near: point])
            def closestCities = datastore.DB.command(geoNearCommand).toMap()
            def closest = (closestCities.results as List<Map>).get(1)
            return new ResponseEntity([name:closest.obj.name, distance:closest.dis/1000], HttpStatus.OK)
        }
        else {
            return new ResponseEntity(HttpStatus.NOT_FOUND)
        }
    }

    @PostConstruct
    void populateCities() {
        datastore.delete(datastore.createQuery(City))
        [new City(name: "London",
                location: new Point(-0.125487, 51.508515)),
         new City(name: "Paris",
                 location: new Point(2.352222, 48.856614)),
         new City(name: "New York",
                 location: new Point(-74.005973, 40.714353)),
         new City(name: "San Francisco",
                 location: new Point(-122.419416, 37.774929))].each {
            datastore.save(it)
        }
        datastore.getCollection(City).createIndex(new BasicDBObject("location", "2dsphere"))
    }

    @Entity
    static class City {
        @Id
        ObjectId id
        String name
        Point location
    }

    static class GeoJsonPointTypeConverter extends TypeConverter {

        GeoJsonPointTypeConverter() {
            super(Point)
        }

        @Override
        Object decode(Class<?> targetClass, Object fromDBObject, MappedField optionalExtraInfo) {
            double[] coordinates = (fromDBObject as DBObject).get("coordinates")
            return new Point(coordinates[0], coordinates[1])
        }

        @Override
        Object encode(Object value, MappedField optionalExtraInfo) {
            def point = value as Point
            return new BasicDBObject([type:"Point", coordinates:[point.coordinates.longitude, point.coordinates.latitude]])
        }
    }
}