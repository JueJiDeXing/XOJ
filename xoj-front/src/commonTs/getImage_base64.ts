import {FileRecordControllerService} from "../../generated";

const getImage_base64 = async (storageName: string) => {
    if (!storageName) throw new Error("空文件名")  ;

    const res = await FileRecordControllerService.getAvatarUsingGet(storageName);
    if (res.code === 0) {
        return `data:image/png;base64,${res.data}`;
    } else {
        throw new Error(res.message);
    }
};
export default getImage_base64;
