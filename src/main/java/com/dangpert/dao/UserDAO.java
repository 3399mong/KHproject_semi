package com.dangpert.dao;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;

import javax.naming.Context;
import javax.naming.InitialContext;

import org.apache.tomcat.dbcp.dbcp2.BasicDataSource;

import com.dangpert.dto.UserDTO;
import com.dangpert.dto.UserDataDTO;

public class UserDAO {

private BasicDataSource bds;
	
	public UserDAO() {
		try {
			Context iCtx = new InitialContext();
			Context envCtx = (Context)iCtx.lookup("java:comp/env");
			bds = (BasicDataSource)envCtx.lookup("jdbc/bds");
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}
	
	
	public int insert(UserDTO dto) throws Exception { // 회원가입
		String sql = "insert into tbl_user values(user_seq.nextval, ?, ?, ?, ?, sysdate, default)";
		
		try(Connection con = bds.getConnection();
			PreparedStatement pstmt = con.prepareStatement(sql)) {
			
			pstmt.setString(1, dto.getUser_id());
			pstmt.setString(2, dto.getUser_pw());
			pstmt.setString(3, dto.getUser_name());
			pstmt.setString(4, dto.getUser_phone());
			
			int rs = pstmt.executeUpdate();
			return rs;
		}
	}
	
	public boolean idCheck(String user_id) throws Exception { // 중복 아이디 확인
		String sql = "select count(*) from tbl_user where user_id=?";
		
		try(Connection con = bds.getConnection();
			PreparedStatement pstmt = con.prepareStatement(sql)) {
			
			pstmt.setString(1, user_id);
			
			ResultSet rs = pstmt.executeQuery();
			int result = 0;
			if (rs.next()) {
				result = rs.getInt(1);
			}
			if(result == 0) {
				return true;
			} else {
				return false;
			}
			
		}
	}
	
	public UserDTO loginOk(String user_id, String user_pw) throws Exception { // 아이디 비밀번호 확인
		String sql = "select * from tbl_user where user_id=? and user_pw=?";
		
		try(Connection con = bds.getConnection();
			PreparedStatement pstmt = con.prepareStatement(sql)) {
			
			pstmt.setString(1, user_id);
			pstmt.setString(2, user_pw);
			
			ResultSet rs = pstmt.executeQuery();
			
			if(rs.next()) {
				int user_seq = rs.getInt("user_seq");
				String user_name = rs.getString("user_name");
				String user_phone = rs.getString("user_phone");
				String signup_date = getStringDate(rs.getDate("signup_date"));
				String user_auth = rs.getString("user_auth");
				String user_memo = rs.getString("user_memo");
				
				return new UserDTO(user_seq, user_id, user_pw, user_name, user_phone, signup_date, user_auth, user_memo);
			} else {
				return null;
			}
		}
	}
	
	public ArrayList<UserDTO> selectAll(int start, int end) throws Exception { // 유저 전체출력
		String sql = "select *"
				+ " from (select tbl_user.*, row_number() over(order by user_seq desc) as num from tbl_user)"
				+ " where num between ? and ?";

		try (Connection con = bds.getConnection(); PreparedStatement pstmt = con.prepareStatement(sql)) {
			pstmt.setInt(1, start);
			pstmt.setInt(2, end);
			
			ResultSet rs = pstmt.executeQuery();
			ArrayList<UserDTO> list = new ArrayList<UserDTO>();

			while (rs.next()) {
				int user_seq = rs.getInt("user_seq");
				String user_id = rs.getString("user_id");
				String user_pw = rs.getString("user_pw");
				String user_name = rs.getString("user_name");
				String user_phone = rs.getString("user_phone");
				String signup_date = getStringDate(rs.getDate("signup_date"));
				String user_auth = rs.getString("user_auth");
				String user_memo = rs.getString("user_memo");

				list.add(new UserDTO(user_seq, user_id, user_pw, user_name, user_phone, signup_date, user_auth,user_memo));
			}
			return list;
		}

	}
	
	public UserDTO selectBySeq(int user_seq) throws Exception {
		String sql = "select * from tbl_user where user_seq=?";
		try(Connection con = bds.getConnection();
			PreparedStatement pstmt = con.prepareStatement(sql)) {
			
			pstmt.setInt(1, user_seq);
			
			ResultSet rs = pstmt.executeQuery();
			rs.next();
			String user_id = rs.getString("user_id");
			String user_name = rs.getString("user_name");
			String user_phone = rs.getString("user_phone");
			String user_auth = rs.getString("user_auth");
			String user_memo = rs.getString("user_memo");
			
			return new UserDTO(user_seq, user_id, null, user_name, user_phone, null, user_auth, user_memo);
			
			
		}
	}
	
	
	
	public String getStringDate(Date date) {
		// 1900년 02월 02일 00시 00분 00초
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy년 MM월 dd일 HH:mm:ss");
		return sdf.format(date);
	}
	
	public HashMap<String, Object> getPageNavi(int curPage) throws Exception{
		String sql = "select count(*) as totalCnt from tbl_user";
		
		try(Connection con = bds.getConnection();
			PreparedStatement pstmt = con.prepareStatement(sql)) {
			
			ResultSet rs = pstmt.executeQuery();
			rs.next();
			
			int totalCnt = rs.getInt("totalCnt"); // 전체 게시글의 개수
			int recordCntPerPage = 10; // 한 페이지에 몇개의 데이터(게시글)을 띄워줄지
			int naviCntPerPage = 5; // 네비를 몇개 단위로 페이징을 구성할지 ex)<1,2,3,4,5>
			int pageTotalCnt = 0; // 총 몇 페이지가 나올지
			
			
			if(totalCnt % recordCntPerPage > 0) { // 나머지가 생김(10의 배수가 아님)
				pageTotalCnt = totalCnt/recordCntPerPage+1;
			} else {
				pageTotalCnt = totalCnt/recordCntPerPage;
			}
			
			if(curPage < 1) { // 현재페이지가 0이거나 혹은 음수이거나
				curPage = 1; // 무조건 첫 페이지로 맞춰주기
			} else if (curPage > pageTotalCnt) { // 현재 페이지가 총 페이지 수보다 크다면
				curPage = pageTotalCnt; // 무조건 마지막 페이지로 맞춰주기
			}
			
			
			
			
			int startNavi = ((curPage-1) / naviCntPerPage)*naviCntPerPage+1;
			int endNavi = startNavi + (naviCntPerPage-1);
			
			if(pageTotalCnt < endNavi) { // endNavi가 전체 페이지 수 보다 크다면
				endNavi = pageTotalCnt; // endNavi를 마지막 페이지로 수정 해주겠다.
			}
			
			
			
			boolean needPrev = true; // startNavi가 1일때 needPrev 가 false
			boolean needNext = true; // endNavi가 마지막 페이지(전체페이지)와 같을때 needNext가 false
			
			if(startNavi == 1) {
				needPrev = false;
			}
			if(endNavi == pageTotalCnt) {
				needNext = false;
			}
			
			HashMap<String, Object> map = new HashMap<>();
			map.put("startNavi", startNavi);
			map.put("endNavi", endNavi);
			map.put("needPrev", needPrev);
			map.put("needNext", needNext);
			
			return map;
		}
		
	}
	
	
}