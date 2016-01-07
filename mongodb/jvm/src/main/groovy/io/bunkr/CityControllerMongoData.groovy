package io.bunkr

import org.bson.types.ObjectId
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.geo.*
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.index.*
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

import javax.annotation.PostConstruct

import static org.springframework.web.bind.annotation.RequestMethod.GET

@RestController
@RequestMapping("/mongodata")
class CityControllerMongoData {

    final CityRepository cityRepository

    @Autowired
    CityControllerMongoData(CityRepository cityRepository) {
        this.cityRepository = cityRepository
    }

    @RequestMapping(value="/", method = GET)
    List<City> index() {
        return cityRepository.findAll()
    }

    @RequestMapping(value="/near/{cityName}", method = GET)
    ResponseEntity nearCity(@PathVariable String cityName) {
        def city = cityRepository.findByName(cityName)
        if(city) {
            GeoResults<City> closestCities = cityRepository.findByLocationNear(city.location, new Distance(10000, Metrics.KILOMETERS))
            def closest = closestCities.content.get(1)
            return new ResponseEntity([name:closest.content.name, distance:closest.distance.in(Metrics.KILOMETERS).value], HttpStatus.OK)
        }
        else {
            return new ResponseEntity(HttpStatus.NOT_FOUND)
        }
    }

    @PostConstruct
    void populateCities() {
        cityRepository.deleteAll()
        [ new City( name:"London",
                location: new Point(-0.125487, 51.508515)),
          new City( name:"Paris",
                  location: new Point(2.352222, 48.856614)),
          new City( name:"New York",
                  location: new Point(-74.005973, 40.714353)),
          new City( name:"San Francisco",
                  location: new Point(-122.419416, 37.774929)) ].each {
            cityRepository.save(it)
        }
    }

    @Document(collection = "city")
    static class City {
        ObjectId id
        String name
        @GeoSpatialIndexed(type = GeoSpatialIndexType.GEO_2DSPHERE)
        Point location
    }
}

interface CityRepository extends MongoRepository<CityControllerMongoData.City, ObjectId> {
    CityControllerMongoData.City findByName(String name);
    GeoResults<CityControllerMongoData.City> findByLocationNear(Point point, Distance distance);
}