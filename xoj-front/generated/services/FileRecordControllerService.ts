/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { BaseResponse_Array_byte_ } from '../models/BaseResponse_Array_byte_';
import type { BaseResponse_string_ } from '../models/BaseResponse_string_';
import type { CancelablePromise } from '../core/CancelablePromise';
import { OpenAPI } from '../core/OpenAPI';
import { request as __request } from '../core/request';
export class FileRecordControllerService {
    /**
     * getAvatar
     * @param storageName storageName
     * @returns BaseResponse_Array_byte_ OK
     * @throws ApiError
     */
    public static getAvatarUsingGet(
        storageName?: string,
    ): CancelablePromise<BaseResponse_Array_byte_> {
        return __request(OpenAPI, {
            method: 'GET',
            url: '/api/file/getAvatar',
            query: {
                'storageName': storageName,
            },
            errors: {
                401: `Unauthorized`,
                403: `Forbidden`,
                404: `Not Found`,
            },
        });
    }
    /**
     * uploadFile
     * @param file
     * @returns BaseResponse_string_ OK
     * @returns any Created
     * @throws ApiError
     */
    public static uploadFileUsingPost(
        file?: Blob,
    ): CancelablePromise<BaseResponse_string_ | any> {
        return __request(OpenAPI, {
            method: 'POST',
            url: '/api/file/upload/avatar',
            formData: {
                'file': file,
            },
            errors: {
                401: `Unauthorized`,
                403: `Forbidden`,
                404: `Not Found`,
            },
        });
    }
}
