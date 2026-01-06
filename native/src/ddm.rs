use crate::WLCState;
use smithay::{
    reexports::{
        wayland_server::{
            protocol::{
                wl_data_device_manager::self as wl_ddm,
                wl_data_device_manager::WlDataDeviceManager as WlDDM,
                wl_data_source::{self, WlDataSource},
                wl_data_device::{self, WlDataDevice},
            },
            DisplayHandle, DataInit, New, GlobalDispatch, Dispatch, Client
        },
    },
};

pub(crate) fn create_ddm_global(disp: &DisplayHandle) {
    disp.create_global::<WLCState, WlDDM, ()>(3, ());
}

impl GlobalDispatch<WlDDM, ()> for WLCState {
    fn bind(
        _state: &mut Self,
        _handle: &DisplayHandle,
        _client: &Client,
        resource: New<WlDDM>,
        _data: &(),
        data_init: &mut DataInit<'_, Self>,
    ) {
        let _ddm: WlDDM = data_init.init(resource, ());
    }
}

impl Dispatch<WlDDM, ()> for WLCState {
    fn request(
        _state: &mut Self,
        _client: &Client,
        _ddm: &WlDDM,
        request: wl_ddm::Request,
        _data: &(),
        _disp: &DisplayHandle,
        data_init: &mut DataInit<'_, Self>,
    ) {
        match request {
            wl_ddm::Request::CreateDataSource { id } => {
                let _data_source = data_init.init(id, ());
            },
            wl_ddm::Request::GetDataDevice { id, .. } => {
                let _data_device = data_init.init(id, ());
            },
            _ => unreachable!(),
        }
    }
}

impl Dispatch<WlDataSource, ()> for WLCState {
    fn request(
        _state: &mut Self,
        _client: &Client,
        _source: &WlDataSource,
        request: wl_data_source::Request,
        _data: &(),
        _disp: &DisplayHandle,
        _data_init: &mut DataInit<'_, Self>,
    ) {
        match request {
            wl_data_source::Request::Offer { .. } => {},
            wl_data_source::Request::Destroy => {},
            wl_data_source::Request::SetActions { .. } => {},
            _ => unreachable!(),
        }
    }
}

impl Dispatch<WlDataDevice, ()> for WLCState {
    fn request(
        _state: &mut Self,
        _client: &Client,
        _device: &WlDataDevice,
        request: wl_data_device::Request,
        _data: &(),
        _disp: &DisplayHandle,
        _data_init: &mut DataInit<'_, Self>,
    ) {
        match request {
            wl_data_device::Request::StartDrag { .. } => {},
            wl_data_device::Request::SetSelection { .. } => {},
            wl_data_device::Request::Release => {},
            _ => unreachable!(),
        }
    }
}
