

import os
import json

if __name__ == '__main__':

    file_list = os.listdir("train/")

    for file in file_list:
        data = json.load(open("train/" + file, "r"))
        # print(data)
        
        
        id_list = []

        data_json = []
        for item in data:
            
            if item["TrainID"] not in id_list:
                id_list.append(item["TrainID"])
                data_json.append(item)
            else:
                raise Exception("Duplicate ID: " + item["TrainID"], file)
                pass

            if item["BeginTime"] >= item["EndTime"]:
                raise Exception("Time Error: ", item, file)
            
        # json.dump(data_json, open("train/" + file, "w"), ensure_ascii=False, indent=4)

