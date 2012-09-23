# Toy Controller

Android toy controller to control arduino [toy server](https://github.com/birdming22/arduino_toy_server)

## API Format
Any Byte except API ID should not be larger than 0x7F.

### API Frame

    ----------------------
    | Id | Len | Payload |
    ----------------------
    
### API ID 

API ID length is one byte.

    0x81: RGB LED Function
    
### Payload Length

Payload length is one byte and max value is 127.
    
### Payload

Payload length is N bytes. N is payload length.
