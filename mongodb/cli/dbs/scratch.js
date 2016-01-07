conn = new Mongo('192.168.99.100:27017');
db = conn.getDB('scratch-db-1');
db.users.insert({'name' : 'name'});
cursor = db.users.find();

var user1 = { test: "test", count: 2 };

user1.test = "test2";

db.users.insert(user1);

user1.count = 5;

db.users.insert(user1);


while ( cursor.hasNext() ) {
    printjson( cursor.next() );
}

//db.users.drop();