import os
import paho.mqtt.client as mqtt

mq_host = os.environ.get('MQTT_HOST')
mq_port = os.environ.get('MQTT_PORT')

def on_connect(client, userdata, flags, rc):
    print("Connected with result code "+str(rc))

    client.subscribe("$SYS/#")


def on_message(client, userdata, msg):
    print(msg.topic+" "+str(msg.payload))

client = mqtt.Client()
client.on_connect = on_connect
client.on_message = on_message

client.connect(mq_host, int(mq_port), 60)

client.loop_forever()