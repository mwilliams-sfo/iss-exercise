
# ISS Exercise Solution

## Technology choices

The following choices have been made generally on the basis of familiarity and Google endorsement.

- UI framework: XML views
- UI architecture: Jetpack MVVM
- View binding: Jetpack binding
- REST calls: Retrofit
- Local database: Room
- Concurrency: Coroutines

No dependency injection framework was used because only one object has dependencies.

The minimum API level is 26, and is determined by availability of ZoneId.ofOffset, which I use to
convert times to EST.

## Additional features

- Locations are requested only while the app is in the foreground.
- "Show on map" button
- The astronaut list is requested each time the app comes to the foreground.
- Efficient trajectory updates with `AsyncListDiffer`
- Unit tests for MainViewModel
