import React, { useState } from 'react'
import { InputBox, SignBorder, CommonStyle } from '../components/Styles'
import { setCharges } from '../api/useUserApi';
import { useMutation } from 'react-query';
import { useNavigate } from 'react-router-dom';

function SetCharging() {
    const navigate = useNavigate();
    const [inputValue, setInputValue] = useState({
        cash: 0,
        msg: "",
    });
    
    const onChangeHandler = (e) => {
        setInputValue({
            ...inputValue,
            [e.target.name]: e.target.value,
        })
    }

    const chargingMutation = useMutation(setCharges, {
        onSuccess: (res)=>{
            if (res == 200) {
                navigate("/get-charging");
            }
        }
    })

    const submitHandler = (e) => {
        e.preventDefault();
        chargingMutation.mutate(inputValue);
    }

  return (
    <div style={ SignBorder }>
        <form onSubmit={submitHandler} style={ CommonStyle }>
            <h1 style={{  fontSize: "80px" }}>Chargin</h1>
            <p>금액과 입금 메세지를 적어주세요</p>
            <div style={{ display: "flex", alignItems: "center", fontSize: "22px", marginTop: "1cm" }}>
                <p style={{ marginRight: "20px"}}>Cash: </p>
                <InputBox type="number" placeholder='금액을 입력해주세요' onChange={onChangeHandler} name="cash"/>
            </div>
            <div style={{ display: "flex", alignItems: "center", fontSize: "22px" }}>
                <p style={{ marginRight: "20px"}}>Msg: </p>
                <InputBox type="text" placeholder='입금 메세지를 입력해주세요' onChange={onChangeHandler} name="msg"/>
            </div>
            <div style={{  marginTop: "1cm", marginLeft: "5cm" }}>
                <button style={{ width: "100px", height: "25px", }}>완료</button>
            </div>
        </form>
    </div>
  )
}

export default SetCharging;